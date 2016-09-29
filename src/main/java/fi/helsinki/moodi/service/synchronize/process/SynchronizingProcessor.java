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

import com.google.common.collect.Lists;
import fi.helsinki.moodi.integration.esb.EsbService;
import fi.helsinki.moodi.integration.moodle.*;
import fi.helsinki.moodi.integration.oodi.OodiCourseUnitRealisation;
import fi.helsinki.moodi.integration.oodi.OodiStudent;
import fi.helsinki.moodi.integration.oodi.OodiTeacher;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.courseEnrollment.CourseEnrollmentStatusService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.util.MapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * Processor implementation that synchronizes courses.
 */
@Component
public class SynchronizingProcessor extends AbstractProcessor {

    private static final String MESSAGE_NOT_CHANGED = "Not changed";
    private static final String MESSAGE_ENROLLMENT_SUCCEEDED = "Enrollment succeeded";
    private static final String MESSAGE_ENROLLMENT_FAILED = "Enrollment failed";
    private static final String MESSAGE_USERNAME_NOT_FOUND = "Username not found from ESB";
    private static final String MESSAGE_MOODLE_USER_NOT_FOUND = "Moodle user not found";
    private static final String MESSAGE_UPDATE_SUCCEEDED = "Enrollment %s succeeded";
    private static final String MESSAGE_UPDATE_FAILED = "Enrollment %s failed";
    private static final String UPDATE_ADD = "add";
    private static final String UPDATE_DROP = "drop";

    private enum SynchronizationAction {
        ADD_ENROLLMENT,
        ADD_ROLE,
        REMOVE_ROLE,
        NONE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizingProcessor.class);

    private final EsbService esbService;
    private final MapperService mapperService;
    private final MoodleService moodleService;
    private final CourseEnrollmentStatusService courseEnrollmentStatusService;
    private final CourseService courseService;

    @Autowired
    public SynchronizingProcessor(EsbService esbService,
                                  MapperService mapperService,
                                  MoodleService moodleService,
                                  CourseEnrollmentStatusService courseEnrollmentStatusService,
                                  CourseService courseService) {
        super(Action.SYNCHRONIZE);
        this.esbService = esbService;
        this.mapperService = mapperService;
        this.moodleService = moodleService;
        this.courseEnrollmentStatusService = courseEnrollmentStatusService;
        this.courseService = courseService;
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
        final SynchronizationItem item,
        final Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {

        final List<EnrollmentSynchronizationItem> enrollmentSynchronizationItems = createSynchronizationItems(item, moodleEnrollmentsByUserId);

        final List<EnrollmentSynchronizationItem> preProcessedItems = enrollmentSynchronizationItems.stream()
            .map(this::checkEnrollmentPrerequisites)
            .collect(Collectors.toList());

        Map<SynchronizationAction, List<EnrollmentSynchronizationItem>> itemsByAction = preProcessedItems.stream()
            .filter(i -> !i.isCompleted())
            .collect(groupingBy(this::getSynchronizationAction));

        List<EnrollmentSynchronizationItem> processedItems = preProcessedItems.stream()
            .filter(EnrollmentSynchronizationItem::isCompleted)
            .collect(Collectors.toList());

        for(SynchronizationAction action : SynchronizationAction.values()) {
            if(itemsByAction.containsKey(action)) {
                processedItems.addAll(processItems(action, itemsByAction.get(action)));
            }
        }

        return processedItems;

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

    private List<EnrollmentSynchronizationItem> processItems(SynchronizationAction action, List<EnrollmentSynchronizationItem> items) {
        switch(action) {
            case ADD_ENROLLMENT:
                return addEnrollments(items);
            case ADD_ROLE:
                return updateEnrollments(items, true);
            case REMOVE_ROLE:
                return updateEnrollments(items, false);
            default:
                return processUnchanged(items);
        }
    }

    private List<EnrollmentSynchronizationItem> createSynchronizationItems(final SynchronizationItem item,
                                                                           final Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {
        final OodiCourseUnitRealisation oodiCourse = item.getOodiCourse().get();

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

    private boolean isAddRole(MoodleUserEnrollments moodleUserEnrollments, long moodleRoleId, boolean approved) {
        return moodleUserEnrollments != null
            && approved
            && !moodleUserEnrollments.hasRole(moodleRoleId);
    }

    private boolean isAddEnrollment(MoodleUserEnrollments moodleUserEnrollments, boolean approved) {
        return moodleUserEnrollments == null
            && approved;
    }

    private boolean isRemoveRole(MoodleUserEnrollments moodleUserEnrollments, long moodleRoleId, boolean approved) {
        return moodleUserEnrollments != null
            && !approved && moodleUserEnrollments.hasRole(moodleRoleId);
    }

    private SynchronizationAction getSynchronizationAction(final EnrollmentSynchronizationItem item) {
        final long moodleRoleId = item.getMoodleRoleId();
        final MoodleUserEnrollments moodleUserEnrollments = item.getMoodleEnrollments().orElse(null);
        final boolean approved = item.isApproved();

        if(isAddRole(moodleUserEnrollments, moodleRoleId, approved)) {
            return SynchronizationAction.ADD_ROLE;
        } else if(isRemoveRole(moodleUserEnrollments, moodleRoleId, approved)) {
            return SynchronizationAction.REMOVE_ROLE;
        } else if(isAddEnrollment(moodleUserEnrollments, approved)) {
            return SynchronizationAction.ADD_ENROLLMENT;
        }
        return SynchronizationAction.NONE;
    }

    private List<String> getUsernameList(OodiStudent student) {
        return esbService.getStudentUsernameList(student.studentNumber);
    }

    private List<String> getUsername(OodiTeacher teacher) {
        return esbService.getTeacherUsernameList(teacher.teacherId);
    }

    private List<EnrollmentSynchronizationItem> updateEnrollments(final List<EnrollmentSynchronizationItem> items, final boolean addition) {
        final String action = addition ? UPDATE_ADD : UPDATE_DROP;
        boolean updateSuccess = false;

        List<MoodleEnrollment> enrollments = items
            .stream()
            .map(item -> new MoodleEnrollment(item.getMoodleRoleId(), item.getMoodleUser().get().id, item.getMoodleCourseId()))
            .collect(Collectors.toList());

        try {
            moodleService.updateEnrollments(enrollments, addition);
            updateSuccess = true;
        } catch (Exception e) {
            LOGGER.error("Error while updating enrollment", e);
        }

        return completeItemsAfterUpdate(items, updateSuccess, action);
    }

    private List<EnrollmentSynchronizationItem> completeItemsAfterUpdate(final List<EnrollmentSynchronizationItem> items, final boolean success, String action) {
        return items
            .stream()
            .map(item -> item.setCompleted(success,
                String.format(success ? MESSAGE_UPDATE_SUCCEEDED : MESSAGE_ENROLLMENT_FAILED, action),
                success ? EnrollmentSynchronizationStatus.COMPLETED : EnrollmentSynchronizationStatus.ERROR))
            .collect(Collectors.toList());
    }

    private Optional<MoodleUser> getMoodleUser(final List<String> usernameList) {
        return moodleService.getUser(usernameList);
    }

    private List<EnrollmentSynchronizationItem> addEnrollments(final List<EnrollmentSynchronizationItem> items) {

        List<MoodleEnrollment> moodleEnrollments = items.stream()
            .map(item -> new MoodleEnrollment(item.getMoodleRoleId(), item.getMoodleUser().get().id, item.getMoodleCourseId()))
            .collect(Collectors.toList());

        try {
            moodleService.enrollToCourse(moodleEnrollments);
            return completeItems(items, true, MESSAGE_ENROLLMENT_SUCCEEDED, EnrollmentSynchronizationStatus.COMPLETED);
        } catch (Exception e) {
            return completeItems(items, false, MESSAGE_ENROLLMENT_FAILED, EnrollmentSynchronizationStatus.ERROR);
        }
    }

    private List<EnrollmentSynchronizationItem> completeItems(List<EnrollmentSynchronizationItem> items,
                                                              boolean success,
                                                              String message,
                                                              EnrollmentSynchronizationStatus status) {
        List<EnrollmentSynchronizationItem> resultItems = Lists.newArrayList();

        for (EnrollmentSynchronizationItem item : items) {
            resultItems.add(item.setCompleted(success, message, status));
        }

        return resultItems;
    }
}
