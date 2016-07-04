package fi.helsinki.moodi.service.synchronize.process;

import com.google.common.collect.Lists;
import fi.helsinki.moodi.integration.esb.EsbService;
import fi.helsinki.moodi.integration.moodle.*;
import fi.helsinki.moodi.integration.oodi.OodiCourseUnitRealisation;
import fi.helsinki.moodi.integration.oodi.OodiStudent;
import fi.helsinki.moodi.integration.oodi.OodiTeacher;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.util.MapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        UPDATE_ENROLLMENT,
        NONE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SynchronizingProcessor.class);

    private final EsbService esbService;
    private final MapperService mapperService;
    private final MoodleService moodleService;

    @Autowired
    public SynchronizingProcessor(EsbService esbService, MapperService mapperService, MoodleService moodleService) {
        super(Action.SYNCHRONIZE);
        this.esbService = esbService;
        this.mapperService = mapperService;
        this.moodleService = moodleService;
    }

    @Override
    protected SynchronizationItem doProcess(final SynchronizationItem item) {

        final Map<Long, MoodleUserEnrollments> moodleEnrollmentsById = groupMoodleEnrollmentsByUserId(item);

        final List<EnrollmentSynchronizationItem> processedItems = synchronizeEnrollments(item, moodleEnrollmentsById);

        final List<StudentSynchronizationItem> processedStudents = filterSynchronizationItemsByType(processedItems, StudentSynchronizationItem.class);

        final List<TeacherSynchronizationItem> processedTeachers = filterSynchronizationItemsByType(processedItems, TeacherSynchronizationItem.class);

        return item.setStudentItems(Optional.of(processedStudents))
            .setTeacherItems(Optional.of(processedTeachers))
            .completeProcessingPhase();
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
            .filter(i -> !i.isCompleted()).collect(groupingBy(this::getSynchronizationAction));

        List<EnrollmentSynchronizationItem> processedItems = preProcessedItems.stream()
            .filter(EnrollmentSynchronizationItem::isCompleted)
            .collect(Collectors.toList());

        for (SynchronizationAction action : itemsByAction.keySet()) {
            processedItems.addAll(processItems(action, itemsByAction.get(action)));
        }

        return processedItems;

    }

    private StudentSynchronizationItem createStudentSynchronizationItem(final OodiStudent student,
                                                                        final MoodleFullCourse moodleCourse,
                                                                        final Map<Long, MoodleUserEnrollments> moodleEnrollmentsByUserId) {
        final StudentSynchronizationItem studentItem = new StudentSynchronizationItem(student,  mapperService.getStudentRoleId(), moodleCourse.id);
        final StudentSynchronizationItem studentItemWithUsername = studentItem.setUsername(getUsername(student));
        final StudentSynchronizationItem studentItemWithMoodleUser =
            studentItemWithUsername.setMoodleUser(studentItemWithUsername.getUsername().flatMap(this::getMoodleUser));

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
            teacherItemWithUsername.setMoodleUser(teacherItemWithUsername.getUsername().flatMap(this::getMoodleUser));

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
                return processAdditions(items);
            case UPDATE_ENROLLMENT:
                return processUpdates(items);
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

    private List<EnrollmentSynchronizationItem> processAdditions(List<EnrollmentSynchronizationItem> items) {
        return addEnrollments(items);
    }

    private List<EnrollmentSynchronizationItem> processUpdates(List<EnrollmentSynchronizationItem> items) {

        List<EnrollmentSynchronizationItem> resultItems = Lists.newArrayList();

        for (EnrollmentSynchronizationItem item : items) {
            resultItems.add(updateEnrollment(item, true));
        }

        return resultItems;
    }

    private EnrollmentSynchronizationItem checkEnrollmentPrerequisites(final EnrollmentSynchronizationItem item) {
        if (!item.getUsername().isPresent()) {
            return item.setCompleted(false, MESSAGE_USERNAME_NOT_FOUND, EnrollmentSynchronizationStatus.USERNAME_NOT_FOUND);
        }

        if (!item.getMoodleUser().isPresent()) {
            return item.setCompleted(false, MESSAGE_MOODLE_USER_NOT_FOUND, EnrollmentSynchronizationStatus.MOODLE_USER_NOT_FOUND);
        }

        return item;
    }

    private SynchronizationAction getSynchronizationAction(final EnrollmentSynchronizationItem item) {

        final long moodleRoleId = item.getMoodleRoleId();
        MoodleUserEnrollments moodleUserEnrollments = item.getMoodleEnrollments().orElse(null);

        if (moodleUserEnrollments != null) {
            if (!moodleUserEnrollments.hasRole(moodleRoleId)) {
                return SynchronizationAction.UPDATE_ENROLLMENT;
            }
        } else {
            return SynchronizationAction.ADD_ENROLLMENT;
        }
        return SynchronizationAction.NONE;
    }

    private Optional<String> getUsername(OodiStudent student) {
        return esbService.getStudentUsername(student.studentNumber);
    }

    private Optional<String> getUsername(OodiTeacher teacher) {
        return esbService.getTeacherUsername(teacher.teacherId);
    }

    private EnrollmentSynchronizationItem updateEnrollment(final EnrollmentSynchronizationItem item, final boolean addition) {
        final MoodleEnrollment enrollment = new MoodleEnrollment(item.getMoodleRoleId(), item.getMoodleUser().get().id, item.getMoodleCourseId());
        final String action = addition ? UPDATE_ADD : UPDATE_DROP;
        try {
            moodleService.updateEnrollments(Lists.newArrayList(enrollment), addition);
            return item.setCompleted(true, String.format(MESSAGE_UPDATE_SUCCEEDED, action), EnrollmentSynchronizationStatus.COMPLETED);
        } catch (Exception e) {
            LOGGER.error("Error while updating enrollment", e);
            return item.setCompleted(false, String.format(MESSAGE_UPDATE_FAILED, action), EnrollmentSynchronizationStatus.ERROR);
        }
    }

    private Optional<MoodleUser> getMoodleUser(final String username) {
        return moodleService.getUser(username);
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
