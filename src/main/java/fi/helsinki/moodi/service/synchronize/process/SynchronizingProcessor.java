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
import fi.helsinki.moodi.integration.moodle.MoodleEnrollment;
import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.integration.moodle.MoodleUser;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryCourseUnitRealisation;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryStudent;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryTeacher;
import fi.helsinki.moodi.service.batch.BatchProcessor;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.iam.IAMService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synclock.SyncLockService;
import fi.helsinki.moodi.service.util.MapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

    private static final Logger logger = LoggerFactory.getLogger(SynchronizingProcessor.class);

    private final IAMService iamService;
    private final MapperService mapperService;
    private final MoodleService moodleService;
    private final CourseService courseService;
    private final SynchronizationThreshold synchronizationThreshold;
    private final SyncLockService syncLockService;
    private final UserSynchronizationActionResolver synchronizationActionResolver;
    private final BatchProcessor<UserSynchronizationAction> batchProcessor;

    @Autowired
    public SynchronizingProcessor(IAMService iamService,
                                  MapperService mapperService,
                                  MoodleService moodleService,
                                  CourseService courseService,
                                  SynchronizationThreshold synchronizationThreshold,
                                  SyncLockService syncLockService,
                                  UserSynchronizationActionResolver synchronizationActionResolver,
                                  BatchProcessor batchProcessor) {
        super(Action.SYNCHRONIZE);
        this.iamService = iamService;
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

    private void completeCourseEnrollments(final SynchronizationItem item) {
        courseService.completeCourseImport(item.getCourse().realisationId, true);
    }

    private List<UserSynchronizationItem> synchronizeUsers(
        SynchronizationItem parentItem,
        Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {

        List<UserSynchronizationItem> userSynchronizationItems = createUserSynchronizationItems(parentItem, moodleEnrollmentsByUserId);

        Map<UserSynchronizationActionType, List<UserSynchronizationAction>> userSynchronizationActionMap = userSynchronizationItems.stream()
            .filter(item -> !item.isCompleted())
            .map(synchronizationActionResolver::enrichWithActions)
            .flatMap(item -> item.getActions().stream())
            .collect(Collectors.groupingBy(item -> item.getActionType()));

        checkThresholdLimits(userSynchronizationActionMap, parentItem);

        for (UserSynchronizationActionType actionType : UserSynchronizationActionType.values()) {
            batchProcessActions(parentItem, actionType, userSynchronizationActionMap.getOrDefault(actionType, newArrayList()));
        }

        return userSynchronizationItems.stream()
            .map(this::completeItem)
            .collect(Collectors.toList());
    }

    private UserSynchronizationItem completeItem(UserSynchronizationItem item) {
        if (item.isCompleted()) {
            return item;
        } else {
            final boolean isSuccess = item.getActions().stream().allMatch(UserSynchronizationAction::isSuccess);
            return isSuccess ? item.withStatus(SUCCESS) : item.withStatus(ERROR);
        }
    }

    private void batchProcessActions(SynchronizationItem parentItem, UserSynchronizationActionType actionType,
                                     List<UserSynchronizationAction> actions) {
        batchProcessor
            .process(
                actions,
                itemsToProcess -> processActions(parentItem, actionType, itemsToProcess),
                ACTION_BATCH_MAX_SIZE);
    }

    private List<UserSynchronizationAction> processActions(SynchronizationItem parentItem,
                                                         UserSynchronizationActionType actionType,
                                                         List<UserSynchronizationAction> actions) {

        List<MoodleEnrollment> moodleEnrollments = actions.stream()
            .flatMap(action -> actionsToMoodleEnrollents(parentItem, action))
            .collect(Collectors.toList());

        try {
            moodleServiceMethodForAction(actionType).accept(moodleEnrollments);
            return actions.stream()
                .map(UserSynchronizationAction::withSuccessStatus)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error(String.format("Error when executing action %s", e));
            return actions.stream()
                .map(UserSynchronizationAction::withErrorStatus)
                .collect(Collectors.toList());
        }
    }

    Consumer<List<MoodleEnrollment>> moodleServiceMethodForAction(UserSynchronizationActionType actionType) {
        switch (actionType) {
            case ADD_ENROLLMENT:
                return moodleService::addEnrollments;
            case SUSPEND_ENROLLMENT:
                return moodleService::suspendEnrollments;
            case REACTIVATE_ENROLLMENT:
                // Adding the enrollment again in Moodle sets the enrollment suspend flag to 0
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

        final StudyRegistryCourseUnitRealisation cur = parentItem.getStudyRegistryCourse().get();
        final long studentCount = cur.students.size();
        final long teacherCount = cur.teachers.size();

        if (!parentItem.isUnlock()) {
            checkThresholdLimitsForRole(itemsByAction, mapperService.getStudentRoleId(), studentCount, parentItem);
            checkThresholdLimitsForRole(itemsByAction, mapperService.getTeacherRoleId(), teacherCount, parentItem);
            // check against students being suspended
            checkThresholdLimitsForRole(itemsByAction, mapperService.getMoodiRoleId(), studentCount, parentItem);
        }
    }

    private void checkThresholdLimitsForRole(Map<UserSynchronizationActionType, List<UserSynchronizationAction>> actionsMap,
                                             long roleId,
                                             long userCountForRole,
                                             SynchronizationItem parentItem) {

        actionsMap.forEach((action, items) -> {
            final long actionCountForRole = items.stream()
                .filter(item -> item.getRoles().contains(roleId))
                .count();

            if (synchronizationThreshold.isLimitedByThreshold(action, actionCountForRole)) {
                lockItem(parentItem, String.format(THRESHOLD_EXCEEDED_MESSAGE, action, actionCountForRole));
            } else if (actionCountForRole == userCountForRole && synchronizationThreshold.isActionPreventedToAllItems(action, actionCountForRole)) {
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

    private List<UserSynchronizationItem> createUserSynchronizationItems(final SynchronizationItem item,
                                                                         final Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {

        final StudyRegistryCourseUnitRealisation course = item.getStudyRegistryCourse().get();

        Stream<UserSynchronizationItem> studentItemStream = course.students
            .stream()
            .map(UserSynchronizationItem::new);
        Stream<UserSynchronizationItem> teacherItemStream = course.teachers
            .stream()
            .map(UserSynchronizationItem::new);

        Map<Boolean, List<UserSynchronizationItem>> userSynchronizationItemsByCompletedStatus = Stream
            .concat(studentItemStream, teacherItemStream)
            .map(i -> i.withMoodleCourseId(item.getCourse().moodleId))
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

    private Function<UserSynchronizationItem, UserSynchronizationItem> enrichWithMoodleUserEnrollments(final Map<Long,
        MoodleUserEnrollments> moodleEnrollmentsByUserId) {
        return userSynchronizationItem ->
            userSynchronizationItem.withMoodleUserEnrollments(moodleEnrollmentsByUserId
                .getOrDefault(userSynchronizationItem.getMoodleUserId(), null));
    }

    private UserSynchronizationItem enrichWithMoodleUser(UserSynchronizationItem item) {
        List<String> usernames = new ArrayList<>();
        if (item.getStudent() != null) {
            if (item.getStudent().userName == null) {
                usernames = getUsernameList(item.getStudent());
            } else {
                usernames.add(item.getStudent().userName);
            }
        }
        if (item.getTeacher() != null) {
            if (item.getTeacher().userName == null) {
                usernames = getUsernameList(item.getTeacher());
            } else {
                usernames.add(item.getTeacher().userName);
            }
        }

        if (usernames.isEmpty()) {
            // https://jira.it.helsinki.fi/browse/MOODI-126
            // Username not found in IAM is no longer considered an error. Should not happen at all with Sisu courses.
            return item;
        }
        List<String> finalUsernames = usernames;
        return getMoodleUser(usernames).map(item::withMoodleUser).orElseGet(() ->  {
            logger.warn("User not found from Moodle with usernames " + finalUsernames);
            return item.withStatus(MOODLE_USER_NOT_FOUND);
        });
    }

    private List<String> getUsernameList(StudyRegistryStudent student) {
        List<String> ret = iamService.getStudentUserNameList(student.studentNumber);
        if (ret.isEmpty()) {
            logger.warn("User not found from IAM with student number " + student.studentNumber);
        }
        return ret;
    }

    private List<String> getUsernameList(StudyRegistryTeacher teacher) {
        List<String> ret = iamService.getTeacherUserNameList(teacher.employeeNumber);
        if (ret.isEmpty()) {
            logger.warn("User not found from IAM with employee number " + teacher.employeeNumber);
        }
        return ret;
    }

    private Optional<MoodleUser> getMoodleUser(final List<String> usernameList) {
        return moodleService.getUser(usernameList);
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

}
