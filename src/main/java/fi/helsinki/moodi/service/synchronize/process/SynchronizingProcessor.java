/*
 * This file is part of Moodi application.
 *
 * Moodi application is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Moodi application is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Moodi application.  If not, see <http://www.gnu.org/licenses/>.
 */

package fi.helsinki.moodi.service.synchronize.process;

import fi.helsinki.moodi.exception.ProcessingException;
import fi.helsinki.moodi.integration.esb.EsbService;
import fi.helsinki.moodi.integration.moodle.*;
import fi.helsinki.moodi.integration.oodi.OodiCourseUsers;
import fi.helsinki.moodi.integration.oodi.OodiStudent;
import fi.helsinki.moodi.integration.oodi.OodiTeacher;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.courseEnrollment.CourseEnrollmentStatusService;
import fi.helsinki.moodi.service.syncLock.SyncLockService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.util.MapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * Processor implementation that synchronizes courses.
 */
@Component
public class SynchronizingProcessor extends AbstractProcessor {

    private static final int ACTION_BATCH_MAX_SIZE = 300;

    private static final String MESSAGE_NOT_CHANGED = "Not changed";
    private static final String MESSAGE_USERNAME_NOT_FOUND = "Username not found from ESB";
    private static final String MESSAGE_MOODLE_USER_NOT_FOUND = "Moodle user not found";
    private static final String THRESHOLD_EXCEEDED_MESSAGE = "Action %s for %s items exceeds threshold";
    private static final String PREVENT_ACTION_ON_ALL_MESSAGE = "Action %s is not permitted for all items";
    private static final String MESSAGE_ACTION_FAILED = "%s failed";
    private static final String MESSAGE_ACTION_SUCCEEDED = "%s succeeded";

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizingProcessor.class);

    private final EsbService esbService;
    private final MapperService mapperService;
    private final MoodleService moodleService;
    private final CourseEnrollmentStatusService courseEnrollmentStatusService;
    private final CourseService courseService;
    private final SynchronizationThreshold synchronizationThreshold;
    private final SyncLockService syncLockService;
    private final SynchronizationActionResolver synchronizationActionResolver;

    @Autowired
    public SynchronizingProcessor(EsbService esbService,
                                  MapperService mapperService,
                                  MoodleService moodleService,
                                  CourseEnrollmentStatusService courseEnrollmentStatusService,
                                  CourseService courseService,
                                  SynchronizationThreshold synchronizationThreshold,
                                  SyncLockService syncLockService,
                                  SynchronizationActionResolver synchronizationActionResolver) {
        super(Action.SYNCHRONIZE);
        this.esbService = esbService;
        this.mapperService = mapperService;
        this.moodleService = moodleService;
        this.courseEnrollmentStatusService = courseEnrollmentStatusService;
        this.courseService = courseService;
        this.synchronizationThreshold = synchronizationThreshold;
        this.syncLockService = syncLockService;
        this.synchronizationActionResolver = synchronizationActionResolver;
    }

    @Override
    protected SynchronizationItem doProcess(final SynchronizationItem item) {

        final Map<Long, MoodleUserEnrollments> moodleEnrollmentsById = groupMoodleEnrollmentsByUserId(item);

        final List<EnrollmentSynchronizationItem> processedItems = synchronizeEnrollments(item, moodleEnrollmentsById);

        final List<StudentSynchronizationItem> processedStudents = filterSynchronizationItemsByType(processedItems, StudentSynchronizationItem.class);

        final List<TeacherSynchronizationItem> processedTeachers = filterSynchronizationItemsByType(processedItems, TeacherSynchronizationItem.class);

        completeCourseEnrollments(item);

        return item.setStudentItems(Optional.of(processedStudents))
            .setTeacherItems(Optional.of(processedTeachers))
            .completeProcessingPhase();
    }

    @Transactional
    private void completeCourseEnrollments(final SynchronizationItem item) {
        courseEnrollmentStatusService.persistCourseEnrollmentStatus(item);
        courseService.completeCourseImport(item.getCourse().realisationId, true);
    }

    private <T extends EnrollmentSynchronizationItem> List<T> filterSynchronizationItemsByType(List<EnrollmentSynchronizationItem> items, Class<T> type) {
        return items.stream().filter(i -> type.isInstance(i)).map(i -> type.cast(i)).collect(Collectors.toList());
    }

    private List<EnrollmentSynchronizationItem> synchronizeEnrollments(
        final SynchronizationItem parentItem,
        final Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {

        final List<EnrollmentSynchronizationItem> enrollmentSynchronizationItems = createSynchronizationItems(parentItem, moodleEnrollmentsByUserId);

        final List<EnrollmentSynchronizationItem> preProcessedItems = enrollmentSynchronizationItems.stream()
            .map(this::checkEnrollmentPrerequisites)
            .collect(Collectors.toList());

        Map<SynchronizationAction, List<EnrollmentSynchronizationItem>> itemsByAction = preProcessedItems.stream()
            .filter(i -> !i.isCompleted())
            .collect(groupingBy(synchronizationActionResolver::resolveSynchronizationAction));

        List<EnrollmentSynchronizationItem> processedItems = preProcessedItems.stream()
            .filter(EnrollmentSynchronizationItem::isCompleted)
            .collect(Collectors.toList());

        processedItems.addAll(checkThresholdsAndProcessItems(itemsByAction, parentItem));

        return processedItems;

    }


    private List<EnrollmentSynchronizationItem> checkThresholdsAndProcessItems(
        Map<SynchronizationAction, List<EnrollmentSynchronizationItem>> itemsByAction, SynchronizationItem parentItem) {

        checkThresholdLimitsForItemType(itemsByAction, StudentSynchronizationItem.class, parentItem);
        checkThresholdLimitsForItemType(itemsByAction, TeacherSynchronizationItem.class, parentItem);

        return processItems(itemsByAction);
    }

    private List<EnrollmentSynchronizationItem> processItems(Map<SynchronizationAction, List<EnrollmentSynchronizationItem>> itemsByAction) {
        List<EnrollmentSynchronizationItem> processedItems = newArrayList();

        for(SynchronizationAction action : SynchronizationAction.values()) {
            if(itemsByAction.containsKey(action)) {
                processedItems.addAll(batchProcessItems(action, itemsByAction.get(action), newArrayList()));
            }
        }

        return processedItems;
    }

    private <T> void checkThresholdLimitsForItemType(Map<SynchronizationAction, List<EnrollmentSynchronizationItem>> itemsByAction,
                                                     Class<T> itemType,
                                                     SynchronizationItem parentItem) {
        Map<SynchronizationAction, List<EnrollmentSynchronizationItem>> itemsOfTypeByAction = new HashMap<>();

        itemsByAction.forEach((action, items) -> {
            itemsOfTypeByAction.put(action, items.stream().filter(i -> itemType.isInstance(i)).collect(Collectors.toList()));
        });

        checkThresholdLimits(itemsOfTypeByAction, parentItem);
    }

    private void checkThresholdLimits(Map<SynchronizationAction, List<EnrollmentSynchronizationItem>> itemsByAction,
                                      SynchronizationItem parentItem) {

        List<EnrollmentSynchronizationItem> allItems = newArrayList();

        itemsByAction.values().forEach(allItems::addAll);

        long allItemsCount = allItems.size();

        itemsByAction.forEach((action, items) -> {
            long itemsCount = items.size();

            if(synchronizationThreshold.isLimitedByThreshold(action, itemsCount)) {
                lockItem(parentItem, String.format(THRESHOLD_EXCEEDED_MESSAGE, action, itemsCount));
            } else if(itemsCount == allItemsCount && synchronizationThreshold.isActionPreventedToAllItems(action, itemsCount)) {
                lockItem(parentItem, String.format(PREVENT_ACTION_ON_ALL_MESSAGE, action));
            }
        });

    }

    private void lockItem(SynchronizationItem parentItem, String message) {
        syncLockService.setLock(parentItem.getCourse(), message);
        throw new ProcessingException(ProcessingStatus.LOCKED, message);
    }

    private StudentSynchronizationItem createStudentSynchronizationItem(final OodiStudent student,
                                                                        final MoodleFullCourse moodleCourse,
                                                                        final Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {
        final StudentSynchronizationItem studentItem = new StudentSynchronizationItem(student,  mapperService.getStudentRoleId(), moodleCourse.id);
        final StudentSynchronizationItem studentItemWithUsername = studentItem.setUsernameList(getUsernameList(student));
        final StudentSynchronizationItem studentItemWithMoodleUser =
            studentItemWithUsername.setMoodleUser(getMoodleUser(studentItemWithUsername.getUsernameList()));

        final StudentSynchronizationItem studentItemWithMoodleEnrollment =
            studentItemWithMoodleUser.setMoodleEnrollments(studentItemWithMoodleUser.getMoodleUser().map(u -> u.id).map(moodleEnrollmentsByUserId::get));

        return studentItemWithMoodleEnrollment;

    }

    private TeacherSynchronizationItem createrTeacherSynchronizationItem(final OodiTeacher teacher,
                                                                         final MoodleFullCourse moodleCourse,
                                                                         final Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {
        final TeacherSynchronizationItem teacherItem = new TeacherSynchronizationItem(teacher, mapperService.getTeacherRoleId(), moodleCourse.id);
        final TeacherSynchronizationItem teacherItemWithUsername = teacherItem.setUsername(getUsername(teacher));
        final TeacherSynchronizationItem teacherItemWithMoodleUser =
            teacherItemWithUsername.setMoodleUser(getMoodleUser(teacherItemWithUsername.getUsernameList()));

        final TeacherSynchronizationItem teacherItemWithMoodleEnrollment =
            teacherItemWithMoodleUser.setMoodleEnrollments(teacherItemWithMoodleUser.getMoodleUser().map(u -> u.id).map(moodleEnrollmentsByUserId::get));

        return teacherItemWithMoodleEnrollment;

    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    private Map<Long, MoodleUserEnrollments> groupMoodleEnrollmentsByUserId(final SynchronizationItem item) {
        final List<MoodleUserEnrollments> enrollments = item.getMoodleEnrollments().get();
        return enrollments.stream().collect(toMap(e -> e.id, Function.identity(), (a, b) -> b));
    }

    private List<EnrollmentSynchronizationItem> batchProcessItems(SynchronizationAction action,
                                                                  List<EnrollmentSynchronizationItem> items,
                                                                  List<EnrollmentSynchronizationItem> completedItems) {
        List<EnrollmentSynchronizationItem> itemsToProcess = items
            .stream()
            .limit(ACTION_BATCH_MAX_SIZE)
            .collect(Collectors.toList());

        if(itemsToProcess.size() > 0) {
            LOGGER.info("Processing action {} for a batch of {} items", action, itemsToProcess.size());
            completedItems.addAll(processItemsByAction(action, itemsToProcess));
            items.removeAll(itemsToProcess);
            return batchProcessItems(action, items, completedItems);
        } else {
            return completedItems;
        }
    }

    private List<EnrollmentSynchronizationItem> processItemsByAction(SynchronizationAction action, List<EnrollmentSynchronizationItem> items) {
        switch(action) {
            case ADD_ENROLLMENT_WITH_MOODI_ROLE:
                return processEnrollments(items, action, this::buildMoodleEnrollmentsWithMoodiRoleEnrollments, moodleService::addEnrollments);
            case ADD_ROLE:
                return processEnrollments(items, action, this::buildMoodleEnrollments, moodleService::addRoles);
            case REMOVE_ROLE:
                return processEnrollments(items, action, this::buildMoodleEnrollments, moodleService::removeRoles);
            case ADD_MOODI_ROLE:
                return processEnrollments(items, action, this::buildMoodiRoleEnrollments, moodleService::addRoles);
            default:
                return processUnchanged(items);
        }
    }

    private MoodleEnrollment createMoodleEnrollment(EnrollmentSynchronizationItem item, long moodleRoleId) {
        return new MoodleEnrollment(
            moodleRoleId,
            item.getMoodleUser().map(user -> user.id).orElseThrow(() -> new RuntimeException("MoodleUser not found!")),
            item.getMoodleCourseId());
    }

    private List<MoodleEnrollment> buildMoodiRoleEnrollments(List<EnrollmentSynchronizationItem> items) {
        return items.stream()
            .map(item -> createMoodleEnrollment(item, mapperService.getMoodiRoleId()))
            .collect(Collectors.toList());
    }

    private List<MoodleEnrollment> buildMoodleEnrollments(List<EnrollmentSynchronizationItem> items) {
        return items.stream()
            .map(item -> createMoodleEnrollment(item, item.getMoodleRoleId()))
            .collect(Collectors.toList());
    }

    private List<MoodleEnrollment> buildMoodleEnrollmentsWithMoodiRoleEnrollments(List<EnrollmentSynchronizationItem> items) {
        return items.stream()
            .flatMap(item -> Stream.of(
                createMoodleEnrollment(item, item.getMoodleRoleId()),
                createMoodleEnrollment(item, mapperService.getMoodiRoleId())))
            .collect(Collectors.toList());
    }

    private List<EnrollmentSynchronizationItem> createSynchronizationItems(final SynchronizationItem item,
                                                                           final Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {
        final OodiCourseUsers oodiCourse = item.getOodiCourse().get();

        final MoodleFullCourse moodleCourse = item.getMoodleCourse().get();

        List<EnrollmentSynchronizationItem> synchronizationItems = oodiCourse.students.stream()
            .map(student -> createStudentSynchronizationItem(student, moodleCourse, moodleEnrollmentsByUserId))
            .collect(Collectors.toList());

        synchronizationItems.addAll(oodiCourse.teachers.stream()
            .map(teacher -> createrTeacherSynchronizationItem(teacher, moodleCourse, moodleEnrollmentsByUserId))
            .collect(Collectors.toList()));

        return synchronizationItems;
    }

    private List<EnrollmentSynchronizationItem> processUnchanged(List<EnrollmentSynchronizationItem> items) {
        return completeItems(items, true, MESSAGE_NOT_CHANGED,  EnrollmentSynchronizationStatus.COMPLETED);
    }

    private EnrollmentSynchronizationItem checkEnrollmentPrerequisites(final EnrollmentSynchronizationItem item) {
        if (item.getUsernameList() == null || item.getUsernameList().size() == 0) {
            return item.setCompleted(false, MESSAGE_USERNAME_NOT_FOUND, EnrollmentSynchronizationStatus.USERNAME_NOT_FOUND);
        }

        if (!item.getMoodleUser().isPresent()) {
            return item.setCompleted(false, MESSAGE_MOODLE_USER_NOT_FOUND, EnrollmentSynchronizationStatus.MOODLE_USER_NOT_FOUND);
        }

        return item;
    }

    private List<String> getUsernameList(OodiStudent student) {
        return esbService.getStudentUsernameList(student.studentNumber);
    }

    private List<String> getUsername(OodiTeacher teacher) {
        return esbService.getTeacherUsernameList(teacher.teacherId);
    }

    private List<EnrollmentSynchronizationItem> processEnrollments(final List<EnrollmentSynchronizationItem> items,
                                                                   SynchronizationAction action,
                                                                   Function<List<EnrollmentSynchronizationItem>, List<MoodleEnrollment>> enrollmentsBuilder,
                                                                   Consumer<List<MoodleEnrollment>> moodleServiceMethod) {
        final List<MoodleEnrollment> moodleEnrollments = enrollmentsBuilder.apply(items);

        boolean success = false;

        try {
            moodleServiceMethod.accept(moodleEnrollments);
            success = true;
        } catch (Exception e) {
            LOGGER.error("Error while updating enrollment", e);
        }

        String message = String.format(success ? MESSAGE_ACTION_SUCCEEDED : MESSAGE_ACTION_FAILED, action.toString());
        EnrollmentSynchronizationStatus status = success ? EnrollmentSynchronizationStatus.COMPLETED : EnrollmentSynchronizationStatus.ERROR;

        return completeItems(items, success, message, status);

    }

    private Optional<MoodleUser> getMoodleUser(final List<String> usernameList) {
        return moodleService.getUser(usernameList);
    }

    private List<EnrollmentSynchronizationItem> completeItems(List<EnrollmentSynchronizationItem> items,
                                                              boolean success,
                                                              String message,
                                                              EnrollmentSynchronizationStatus status) {
        return items.stream()
            .map(item -> item.setCompleted(success, message, status))
            .collect(Collectors.toList());
    }
}
