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

package fi.helsinki.moodi.service.courseEnrollment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import fi.helsinki.moodi.service.course.CourseRepository;
import fi.helsinki.moodi.service.dto.CourseEnrollmentStatusDto;
import fi.helsinki.moodi.service.dto.converter.CourseEnrollmentStatusDtoConverter;
import fi.helsinki.moodi.service.importing.Enrollment;
import fi.helsinki.moodi.service.importing.EnrollmentWarning;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.StudentSynchronizationItem;
import fi.helsinki.moodi.service.synchronize.process.TeacherSynchronizationItem;
import fi.helsinki.moodi.service.time.TimeService;
import fi.helsinki.moodi.service.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseEnrollmentStatusService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentStatusRepository courseEnrollmentStatusRepository;
    private final TimeService timeService;
    private final CourseEnrollmentStatusDtoConverter courseEnrollmentStatusDtoConverter;

    private final JsonUtil jsonUtil;

    @Autowired
    public CourseEnrollmentStatusService(CourseRepository courseRepository,
                                         CourseEnrollmentStatusRepository courseEnrollmentStatusRepository,
                                         TimeService timeService,
                                         JsonUtil jsonUtil,
                                         CourseEnrollmentStatusDtoConverter courseEnrollmentStatusDtoConverter) {
        this.courseRepository = courseRepository;
        this.courseEnrollmentStatusRepository = courseEnrollmentStatusRepository;
        this.timeService = timeService;
        this.jsonUtil = jsonUtil;
        this.courseEnrollmentStatusDtoConverter = courseEnrollmentStatusDtoConverter;
    }

    public CourseEnrollmentStatusDto getCourseEnrollmentStatus(Long realisationId) {
        return courseEnrollmentStatusRepository.findTop1ByRealisationIdOrderByCreatedDesc(realisationId)
            .map(courseEnrollmentStatusDtoConverter::toDto)
            .orElse(null);
    }

    private List<StudentEnrollmentStatus> createStudentEnrollmentStatuses(List<StudentSynchronizationItem> studentSynchronizationItems) {
        return studentSynchronizationItems.stream()
            .map(StudentEnrollmentStatus::new)
            .collect(Collectors.toList());
    }

    private List<TeacherEnrollmentStatus> createTeacherEnrollmentStatuses(List<TeacherSynchronizationItem> teacherSynchronizationItems) {
        return teacherSynchronizationItems.stream()
            .map(TeacherEnrollmentStatus::new)
            .collect(Collectors.toList());
    }

    private <T> String writeEnrollmentStatusesToString(List<T> enrollmentStatuses) {
        try {
            return jsonUtil.objectToJsonString(enrollmentStatuses);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private CourseEnrollmentStatus createCourseEnrolmentStatusFromSynchronizationItem(SynchronizationItem synchronizationItem) {
        CourseEnrollmentStatus courseEnrollmentStatus = new CourseEnrollmentStatus();
        courseEnrollmentStatus.courseId = synchronizationItem.getCourse().id;
        courseEnrollmentStatus.realisationId = synchronizationItem.getCourse().realisationId;
        courseEnrollmentStatus.created = timeService.getCurrentDateTime();

        courseEnrollmentStatus.studentEnrollments = synchronizationItem.getStudentItems()
                .map(this::createStudentEnrollmentStatuses)
                .map(this::writeEnrollmentStatusesToString)
                .orElse(null);

        courseEnrollmentStatus.teacherEnrollments = synchronizationItem.getTeacherItems()
            .map(this::createTeacherEnrollmentStatuses)
            .map(this::writeEnrollmentStatusesToString)
            .orElse(null);

       return courseEnrollmentStatus;
    }

    public void persistCourseEnrollmentStatus(SynchronizationItem synchronizationItem) {
        courseEnrollmentStatusRepository.save(createCourseEnrolmentStatusFromSynchronizationItem(synchronizationItem));
    }

    public CourseEnrollmentStatus persistCourseEnrollmentStatus(
        Long courseId,
        Long realisationId,
        List<Enrollment> enrollments,
        List<EnrollmentWarning> enrollmentWarnings) {

        CourseEnrollmentStatus courseEnrollmentStatus = new CourseEnrollmentStatus();
        courseEnrollmentStatus.courseId = courseId;
        courseEnrollmentStatus.realisationId = realisationId;
        courseEnrollmentStatus.created = timeService.getCurrentDateTime();

        Map<String, List<Enrollment>> successFullEnrollementsByRole = enrollments.stream()
            .filter(e -> enrollmentWarnings.stream().noneMatch(w -> w.enrollment.equals(e)))
            .collect(Collectors.groupingBy(e -> e.role));

        Map<String, List<EnrollmentWarning>> failedEnrollmentsByRole = enrollmentWarnings.stream()
            .collect(Collectors.groupingBy(w -> w.enrollment.role));

        List<StudentEnrollmentStatus> studentEnrollmentStatuses = Lists.newArrayList();
        List<TeacherEnrollmentStatus> teacherEnrollmentStatuses = Lists.newArrayList();

        successFullEnrollementsByRole
            .getOrDefault(Enrollment.ROLE_STUDENT, Lists.newArrayList()).stream()
            .map(StudentEnrollmentStatus::new)
            .forEach(studentEnrollmentStatuses::add);

        successFullEnrollementsByRole
            .getOrDefault(Enrollment.ROLE_TEACHER, Lists.newArrayList()).stream()
            .map(TeacherEnrollmentStatus::new)
            .forEach(teacherEnrollmentStatuses::add);

        failedEnrollmentsByRole
            .getOrDefault(Enrollment.ROLE_STUDENT, Lists.newArrayList()).stream()
            .map(StudentEnrollmentStatus::new)
            .forEach(studentEnrollmentStatuses::add);

        failedEnrollmentsByRole
            .getOrDefault(Enrollment.ROLE_TEACHER, Lists.newArrayList()).stream()
            .map(TeacherEnrollmentStatus::new)
            .forEach(teacherEnrollmentStatuses::add);

        courseEnrollmentStatus.studentEnrollments = writeEnrollmentStatusesToString(studentEnrollmentStatuses);
        courseEnrollmentStatus.teacherEnrollments = writeEnrollmentStatusesToString(teacherEnrollmentStatuses);

        return courseEnrollmentStatusRepository.save(courseEnrollmentStatus);
    }

}
