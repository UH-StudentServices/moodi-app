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
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryTeacher;
import fi.helsinki.moodi.service.batch.BatchProcessor;
import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.enrich.EnricherService;
import fi.helsinki.moodi.service.synclock.SyncLockService;
import fi.helsinki.moodi.service.util.MapperService;
import io.micrometer.core.instrument.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.service.synchronize.process.UserSynchronizationItem.UserSynchronizationItemStatus.*;

/**
 * Synchronization for one course, represented by synchronizationItem.
 */
@Component
public class SynchronizingProcessor {

    private static final int ACTION_BATCH_MAX_SIZE = 300;

    private static final String THRESHOLD_EXCEEDED_MESSAGE = "Action %s for %s items exceeds threshold";
    private static final String PREVENT_ACTION_ON_ALL_MESSAGE = "Action %s is not permitted for all items";

    private static final Logger logger = LoggerFactory.getLogger(SynchronizingProcessor.class);

    private final MapperService mapperService;
    private final MoodleService moodleService;
    private final CourseService courseService;
    private final EnricherService enricherService;
    private final SynchronizationThreshold synchronizationThreshold;
    private final SyncLockService syncLockService;
    private final UserSynchronizationActionResolver synchronizationActionResolver;
    private final BatchProcessor<UserSynchronizationAction> batchProcessor;

    @Autowired
    public SynchronizingProcessor(MapperService mapperService,
                                  MoodleService moodleService,
                                  CourseService courseService,
                                  EnricherService enricherService,
                                  SynchronizationThreshold synchronizationThreshold,
                                  SyncLockService syncLockService,
                                  UserSynchronizationActionResolver synchronizationActionResolver,
                                  BatchProcessor batchProcessor) {
        this.mapperService = mapperService;
        this.moodleService = moodleService;
        this.courseService = courseService;
        this.enricherService = enricherService;
        this.synchronizationThreshold = synchronizationThreshold;
        this.syncLockService = syncLockService;
        this.synchronizationActionResolver = synchronizationActionResolver;
        this.batchProcessor = batchProcessor;
    }

    public SynchronizationItem doSynchronize(final SynchronizationItem item) {

        final Map<Long, MoodleUserEnrollments> moodleEnrollmentsById = groupMoodleEnrollmentsByUserId(item);

        final List<UserSynchronizationItem> processedItems = synchronizeUsers(item, moodleEnrollmentsById);

        completeCourseEnrollments(item);
        item.setUserSynchronizationItems(processedItems);
        item.completeProcessingPhase();
        return item;
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
            .collect(Collectors.groupingBy(UserSynchronizationAction::getActionType));
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
            switch (actionType) {
                case ADD_ENROLLMENT:
                case REACTIVATE_ENROLLMENT: // Adding the enrollment again in Moodle sets the enrollment suspend flag to 0
                    moodleService.addEnrollments(moodleEnrollments);
                    break;
                case SUSPEND_ENROLLMENT:
                    moodleService.suspendEnrollments(moodleEnrollments);
                    break;
                case ADD_ROLES:
                    moodleService.addRoles(moodleEnrollments);
                    break;
                case REMOVE_ROLES:
                    moodleService.removeRoles(moodleEnrollments);
                    break;
                default:
                    throw new IllegalArgumentException("No service method mapped for action: " + actionType);
            }
            return actions.stream()
                .map(UserSynchronizationAction::withSuccessStatus)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error(String.format("Error when executing action %s", e), e);
            return actions.stream()
                .map(UserSynchronizationAction::withErrorStatus)
                .collect(Collectors.toList());
        }
    }

    private void checkThresholdLimits(Map<UserSynchronizationActionType, List<UserSynchronizationAction>> itemsByAction,
                                      SynchronizationItem parentItem) {

        final StudyRegistryCourseUnitRealisation cur = parentItem.getStudyRegistryCourse();
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
        final List<MoodleUserEnrollments> enrollments = item.getMoodleEnrollments();
        return enrollments.stream().collect(Collectors.toMap(e -> e.id, Function.identity(), (a, b) -> b));
    }

    private Stream<MoodleEnrollment> actionsToMoodleEnrollents(SynchronizationItem parentItem, UserSynchronizationAction action) {
        return action
            .getRoles()
            .stream()
            .map(role -> new MoodleEnrollment(role, action.getMoodleUserId(), parentItem.getMoodleCourse().id));
    }

    private List<UserSynchronizationItem> createUserSynchronizationItems(final SynchronizationItem item,
                                                                         final Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {

        final StudyRegistryCourseUnitRealisation course = item.getStudyRegistryCourse();

        List<UserSynchronizationItem> personItems = new ArrayList<>();

        personItems.addAll(course.students
            .stream()
            .map(UserSynchronizationItem::new).collect(Collectors.toList()));
        personItems.addAll(course.teachers
            .stream()
            .map(UserSynchronizationItem::new).collect(Collectors.toList()));
        if (item.getCourse().creatorUsername != null) {
            StudyRegistryTeacher creator = new StudyRegistryTeacher();
            creator.userName = item.getCourse().creatorUsername;
            personItems.add(new UserSynchronizationItem(creator));
        }

        Map<Boolean, List<UserSynchronizationItem>> userSynchronizationItemsByCompletedStatus =
            personItems.stream()
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

        Set<String> studyRegistryStudentUsernames = course.students.stream().map(s -> s.userName).collect(Collectors.toSet());

        // https://jira.it.helsinki.fi/browse/MOODI-122 process Moodle users that do not appear in StudyRegistry.
        Stream<UserSynchronizationItem> moodleUsersNotInStudyRegistry =
            moodleEnrollmentsByUserId.values().stream().filter(m -> m.username != null & !studyRegistryStudentUsernames.contains(m.username))
            .map(m -> new UserSynchronizationItem(m).withMoodleCourseId(item.getCourse().moodleId));

        return Stream.concat(
            moodleUsersNotInStudyRegistry,
            Stream.concat(
                combinedUncompletedItems.stream().map(enrichWithMoodleUserEnrollments(moodleEnrollmentsByUserId)),
                completedItems.stream())
        ).collect(Collectors.toList());
    }

    private Function<UserSynchronizationItem, UserSynchronizationItem> enrichWithMoodleUserEnrollments(final Map<Long,
        MoodleUserEnrollments> moodleEnrollmentsByUserId) {
        return userSynchronizationItem ->
            userSynchronizationItem.withMoodleUserEnrollments(moodleEnrollmentsByUserId
                .getOrDefault(userSynchronizationItem.getMoodleUserId(), null));
    }

    private UserSynchronizationItem enrichWithMoodleUser(UserSynchronizationItem item) {
        List<String> usernames = new ArrayList<>();
        if (item.getStudent() != null && StringUtils.isNotEmpty(item.getStudent().userName)) {
            usernames.add(item.getStudent().userName);
        }
        if (item.getTeacher() != null && StringUtils.isNotEmpty(item.getTeacher().userName)) {
            usernames.add(item.getTeacher().userName);
        }

        if (usernames.isEmpty()) {
            // Some users do not have a username, and thus cannot be synced to Moodle.
            // This is not considered an error.
            return item.withStatus(SUCCESS);
        }
        return getMoodleUser(usernames).map(item::withMoodleUser).orElseGet(() ->  {
            logger.warn("User not found from Moodle with usernames " + usernames);
            return item.withStatus(MOODLE_USER_NOT_FOUND);
        });
    }

    private Optional<MoodleUser> getMoodleUser(final List<String> usernameList) {
        Optional<MoodleUser> moodleUser = enricherService.getPrefetchedMoodleUser(usernameList);
        if (!moodleUser.isPresent()) {
            moodleUser = moodleService.getUser(usernameList);
        }
        return moodleUser;
    }
}
