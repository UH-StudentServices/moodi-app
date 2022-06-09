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

package fi.helsinki.moodi.service.log;

import fi.helsinki.moodi.service.importing.Enrollment;
import fi.helsinki.moodi.service.importing.EnrollmentWarning;
import fi.helsinki.moodi.service.log.ImportEnrollmentsSummaryLog.StudentEnrollmentEntry;
import fi.helsinki.moodi.service.log.ImportEnrollmentsSummaryLog.TeacherEnrollmentEntry;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.service.importing.EnrollmentWarning.CODE_ENROLLMENT_FAILED;
import static org.junit.Assert.assertEquals;

public class ImportEnrollmentsSummaryLogTest extends AbstractSummaryLogTest {

    @Test
    public void thatImportSummaryLogIsCreated() {

        Enrollment studentEnrollment = Enrollment.forStudent(STUDENT_NUMBER);
        Enrollment teacherEnrollment = Enrollment.forTeacher(TEACHER_ID, null);

        List<Enrollment> enrollments = newArrayList(studentEnrollment, teacherEnrollment);

        List<EnrollmentWarning> enrollmentWarnings = newArrayList(new EnrollmentWarning(CODE_ENROLLMENT_FAILED, teacherEnrollment));

        ImportEnrollmentsSummaryLog importEnrollmentsSummaryLog = new ImportEnrollmentsSummaryLog(enrollments, enrollmentWarnings);

        assertEquals(1, importEnrollmentsSummaryLog.summary.enrolledStudents);
        assertEquals(0, importEnrollmentsSummaryLog.summary.enrolledTeachers);
        assertEquals(0, importEnrollmentsSummaryLog.summary.failedStudents.size());
        assertEquals(1, importEnrollmentsSummaryLog.summary.failedTeachers.size());
        assertEquals(1, importEnrollmentsSummaryLog.summary.failedTeachers.get(CODE_ENROLLMENT_FAILED).intValue());
        assertEquals(1, importEnrollmentsSummaryLog.enrolledStudents.size());
        assertEquals(0, importEnrollmentsSummaryLog.enrolledTeachers.size());
        assertEquals(0, importEnrollmentsSummaryLog.failedStudents.size());
        assertEquals(1, importEnrollmentsSummaryLog.failedTeachers.size());
        assertEquals(STUDENT_NUMBER, ((StudentEnrollmentEntry) importEnrollmentsSummaryLog.enrolledStudents
            .get(0)).studentNumber);
        assertEquals(TEACHER_ID, ((TeacherEnrollmentEntry) importEnrollmentsSummaryLog.failedTeachers
            .get(CODE_ENROLLMENT_FAILED).get(0)).employeeNumber);

    }
}
