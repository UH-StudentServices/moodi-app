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
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.oodi.OodiCourseUsers;
import fi.helsinki.moodi.integration.oodi.OodiStudent;
import fi.helsinki.moodi.integration.oodi.OodiTeacher;
import fi.helsinki.moodi.service.batch.BatchProcessor;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.syncLock.SyncLockService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.util.MapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem.UserSynchronizationItemStatus.*;

/**
 * Processor implementation that synchronizes courses.
 */
@Component
public class SynchronizingProcessor extends AbstractProcessor {

    private static final int ACTION_BATCH_MAX_SIZE = 300;

    private static final String THRESHOLD_EXCEEDED_MESSAGE = "Action %s for %s items exceeds threshold";
    private static final String PREVENT_ACTION_ON_ALL_MESSAGE = "Action %s is not permitted for all items";

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizingProcessor.class);

    private final EsbService esbService;
    private final MapperService mapperService;
    private final MoodleService moodleService;
    private final CourseService courseService;
    private final SynchronizationThreshold synchronizationThreshold;
    private final SyncLockService syncLockService;
    private final UserSynchronizationActionResolver synchronizationActionResolver;
    private final BatchProcessor<UserSynchronizationAction> batchProcessor;

    @Autowired
    public SynchronizingProcessor(EsbService esbService,
                                  MapperService mapperService,
                                  MoodleService moodleService,
                                  CourseService courseService,
                                  SynchronizationThreshold synchronizationThreshold,
                                  SyncLockService syncLockService,
                                  UserSynchronizationActionResolver synchronizationActionResolver,
                                  BatchProcessor batchProcessor) {
        super(Action.SYNCHRONIZE);
        this.esbService = esbService;
        this.mapperService = mapperService;
        this.moodleService = moodleService;
        this.courseService = courseService;
        this.synchronizationThreshold = synchronizationThreshold;
        this.syncLockService = syncLockService;
        this.synchronizationActionResolver = synchronizationActionResolver;
        this.batchProcessor = batchProcessor;
    }

    @Override
    protected SynchronizationItem doProcess(final SynchronizationItem item) {

        final Map<Long, MoodleUserEnrollments> moodleEnrollmentsById = groupMoodleEnrollmentsByUserId(item);

        final List<UserSynchronizationItem> processedItems = synchronizeUsers(item, moodleEnrollmentsById);

        completeCourseEnrollments(item);

        return item
            .setUserSynchronizationItems(processedItems)
            .completeProcessingPhase();
    }

    @Transactional
    private void completeCourseEnrollments(final SynchronizationItem item) {
        courseService.completeCourseImport(item.getCourse().realisationId, true);
    }

    private List<UserSynchronizationItem> synchronizeUsers(
        SynchronizationItem parentItem,
        Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {

        List<UserSynchronizationItem> userSynchronizationItems = createUserSyncronizationItems(parentItem, moodleEnrollmentsByUserId);

        Map<UserSynchronizationActionType, List<UserSynchronizationAction>> userSynchronizationActionMap = userSynchronizationItems.stream()
            .filter(item -> !item.isCompleted())
            .map(synchronizationActionResolver::enrichWithActions)
            .flatMap(item -> item.getActions().stream())
            .collect(Collectors.groupingBy(item -> item.getActionType()));

        checkThresholdLimits(userSynchronizationActionMap, parentItem);

        for(UserSynchronizationActionType actionType : UserSynchronizationActionType.values()) {
            batchProcessActions(parentItem, actionType, userSynchronizationActionMap.getOrDefault(actionType, newArrayList()));
        }

        return userSynchronizationItems.stream()
            .map(this::completeItem)
            .collect(Collectors.toList());
    }

    private UserSynchronizationItem completeItem(UserSynchronizationItem item) {
        if(item.isCompleted()) {
            return item;
        } else {
            final boolean isSuccess = item.getActions().stream().allMatch(UserSynchronizationAction::isSuccess);
            return isSuccess ? item.withStatus(SUCCESS) : item.withStatus(ERROR);
        }
    }

    private void batchProcessActions(SynchronizationItem parentItem, UserSynchronizationActionType actionType, List<UserSynchronizationAction> actions) {
        batchProcessor
            .process(
                actions,
                itemsToProcess -> processActions(parentItem, actionType, itemsToProcess),
                ACTION_BATCH_MAX_SIZE);
    }

    private List<UserSynchronizationAction> processActions(SynchronizationItem parentItem,
                                                         UserSynchronizationActionType actionType,
                                                         List<UserSynchronizationAction> actions){

        List<MoodleEnrollment> moodleEnrollments = actions.stream()
            .flatMap(action -> actionsToMoodleEnrollents(parentItem, action))
            .collect(Collectors.toList());

        try {
            moodleServiceMethodForAction(actionType).accept(moodleEnrollments);
            return actions.stream()
                .map(UserSynchronizationAction::withSuccessStatus)
                .collect(Collectors.toList());

        } catch (Exception e) {
            LOGGER.error(String.format("Error when executing action %s", e));
            return actions.stream()
                .map(UserSynchronizationAction::withErrorStatus)
                .collect(Collectors.toList());
        }
    }

    Consumer<List<MoodleEnrollment>> moodleServiceMethodForAction(UserSynchronizationActionType actionType) {
        switch (actionType) {
            case ADD_ENROLLMENT:
                return moodleService::addEnrollments;
            case ADD_ROLES:
                return moodleService::addRoles;
            case REMOVE_ROLES:
                return moodleService::removeRoles;
            default:
                throw new IllegalArgumentException("No service method mapped for action: " + actionType.toString());
        }
    }

    private void checkThresholdLimits(Map<UserSynchronizationActionType, List<UserSynchronizationAction>> itemsByAction,
                                      SynchronizationItem parentItem) {

        final OodiCourseUsers oodiCourse = parentItem.getOodiCourse().get();
        final long studentCount = oodiCourse.students.size();
        final long teacherCount = oodiCourse.teachers.size();

        checkThresholdLimitsForRole(itemsByAction, mapperService.getStudentRoleId(), studentCount, parentItem);
        checkThresholdLimitsForRole(itemsByAction, mapperService.getTeacherRoleId(), teacherCount, parentItem);

    }

    private void checkThresholdLimitsForRole(Map<UserSynchronizationActionType, List<UserSynchronizationAction>> actionsMap,
                                             long roleId,
                                             long userCountForRole,
                                             SynchronizationItem parentItem) {

        actionsMap.forEach((action, items) -> {
            final long actionCountForRole = items.stream()
                .filter(item -> item.getRoles().contains(roleId))
                .count();

            if(synchronizationThreshold.isLimitedByThreshold(action, actionCountForRole)) {
                lockItem(parentItem, String.format(THRESHOLD_EXCEEDED_MESSAGE, action, actionCountForRole));
            } else if(actionCountForRole == userCountForRole && synchronizationThreshold.isActionPreventedToAllItems(action, actionCountForRole)) {
                lockItem(parentItem, String.format(PREVENT_ACTION_ON_ALL_MESSAGE, action));
            }
        });

    }

    private void lockItem(SynchronizationItem parentItem, String message) {
        syncLockService.setLock(parentItem.getCourse(), message);
        throw new ProcessingException(ProcessingStatus.LOCKED, message);
    }

    private Map<Long, MoodleUserEnrollments> groupMoodleEnrollmentsByUserId(final SynchronizationItem item) {
        final List<MoodleUserEnrollments> enrollments = item.getMoodleEnrollments().get();
        return enrollments.stream().collect(Collectors.toMap(e -> e.id, Function.identity(), (a, b) -> b));
    }

    private Stream<MoodleEnrollment> actionsToMoodleEnrollents(SynchronizationItem parentItem, UserSynchronizationAction action) {
        return action
            .getRoles()
            .stream()
            .map(role -> new MoodleEnrollment(role, action.getMoodleUserId(), parentItem.getMoodleCourse().get().id));
    }

    private List<UserSynchronizationItem> createUserSyncronizationItems(final SynchronizationItem item,
                                                                        final Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {

        final OodiCourseUsers oodiCourse = item.getOodiCourse().get();

        Stream<UserSynchronizationItem> studentItemStream = oodiCourse.students
            .stream()
            .map(UserSynchronizationItem::new);
        Stream<UserSynchronizationItem> teacherItemStream = oodiCourse.teachers
            .stream()
            .map(UserSynchronizationItem::new);

        Map<Boolean, List<UserSynchronizationItem>> userSynchronizationItemsByCompletedStatus = Stream
            .concat(studentItemStream, teacherItemStream)
            .map(this::enrichWithMoodleUser)
            .collect(Collectors.groupingBy(UserSynchronizationItem::isCompleted));

        List<UserSynchronizationItem> completedItems = userSynchronizationItemsByCompletedStatus.getOrDefault(true, newArrayList());
        List<UserSynchronizationItem> unCompletedItems = userSynchronizationItemsByCompletedStatus.getOrDefault(false, newArrayList());

        Collection<UserSynchronizationItem> combinedUncompletedItems = unCompletedItems.stream()
            .collect(Collectors.toMap(
                UserSynchronizationItem::getMoodleUserId,
                Function.identity(),
                UserSynchronizationItem::combine))
            .values();

        return Stream.concat(
            combinedUncompletedItems.stream().map(enrichWithMoodleUserEnrollments(moodleEnrollmentsByUserId)),
            completedItems.stream()).collect(Collectors.toList());
    }

    private Function<UserSynchronizationItem, UserSynchronizationItem> enrichWithMoodleUserEnrollments(final Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {
        return userSynchronizationItem ->
            userSynchronizationItem.withMoodleUserEnrollments(moodleEnrollmentsByUserId
                .getOrDefault(userSynchronizationItem.getMoodleUserId(), null));
    }

    private UserSynchronizationItem enrichWithMoodleUser(UserSynchronizationItem item) {
        List<String> usernames = item.getOodiStudent() != null ? getUsernameList(item.getOodiStudent()) : getUsernameList(item.getOodiTeacher());

        if(usernames.isEmpty()) {
            return item.withStatus(USERNAME_NOT_FOUND);
        }
        return getMoodleUser(usernames).map(item::withMoodleUser).orElseGet(() -> item.withStatus(MOODLE_USER_NOT_FOUND));
    }

    private List<String> getUsernameList(OodiStudent student) {
        return esbService.getStudentUsernameList(student.studentNumber);
    }

    private List<String> getUsernameList(OodiTeacher teacher) {
        return esbService.getTeacherUsernameList(teacher.teacherId);
    }

    private Optional<MoodleUser> getMoodleUser(final List<String> usernameList) {
        return moodleService.getUser(usernameList);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
