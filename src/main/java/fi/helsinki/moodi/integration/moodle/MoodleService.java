package fi.helsinki.moodi.integration.moodle;

import fi.helsinki.moodi.service.util.MapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MoodleService {

    private final MoodleClient moodleClient;
    private final MapperService mapperService;

    @Autowired
    public MoodleService(MoodleClient moodleClient, MapperService mapperService) {
        this.moodleClient = moodleClient;
        this.mapperService = mapperService;
    }

    public long createCourse(final MoodleCourse course) {
        return moodleClient.createCourse(course);
    }

    public Optional<MoodleUser> getUser(final String username) {
        return Optional.ofNullable(moodleClient.getUser(username));
    };

    public void enrollToCourse(final List<MoodleEnrollment> moodleEnrollments) {
        moodleClient.enrollToCourse(addEnrollmentsToDefaultMoodiRole(moodleEnrollments));
    }

    private List<MoodleEnrollment> addEnrollmentsToDefaultMoodiRole(final List<MoodleEnrollment> moodleEnrollments) {
        return moodleEnrollments.stream()
            .flatMap(enrollment -> Stream.of(
                enrollment,
                new MoodleEnrollment(mapperService.getMoodiRoleId(), enrollment.moodleUserId, enrollment.moodleCourseId)))
            .collect(Collectors.toList());
    }

    public long createUser(final String username, final String firstName, final String lastName,
                           final String email, final String password, final String idNumber) {
        return moodleClient.createUser(username, firstName, lastName, email, password, idNumber);
    }

    public void deleteUser(final long userId) {
        moodleClient.deleteUser(userId);
    }

    public List<MoodleFullCourse> getCourses(final List<Long> courseIds) {
        return moodleClient.getCourses(courseIds);
    }

    public List<MoodleUserEnrollments> getEnrolledUsers(final long courseId) {
        return moodleClient.getEnrolledUsers(courseId);
    }

    public void updateEnrollments(final List<MoodleEnrollment> moodleEnrollments, final boolean addition) {
        moodleClient.updateEnrollments(moodleEnrollments, addition);
    }
}
