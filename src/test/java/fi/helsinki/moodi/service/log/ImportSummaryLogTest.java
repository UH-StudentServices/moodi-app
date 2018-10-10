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
import fi.helsinki.moodi.service.log.ImportSummaryLog.StudentEnrollmentEntry;
import fi.helsinki.moodi.service.log.ImportSummaryLog.TeacherEnrollmentEntry;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static fi.helsinki.moodi.service.importing.EnrollmentWarning.CODE_ENROLLMENT_FAILED;
import static org.junit.Assert.assertEquals;

public class ImportSummaryLogTest extends AbstractSummaryLogTest {

    @Test
    public void thatImportSummaryLogIsCreated() {

        Enrollment studentEnrollment = Enrollment.forStudent(STUDENT_NUMBER);
        Enrollment teacherEnrollment = Enrollment.forTeacher(TEACHER_ID);

        List<Enrollment> enrollments = newArrayList(studentEnrollment, teacherEnrollment);

        List<EnrollmentWarning> enrollmentWarnings = newArrayList(new EnrollmentWarning(CODE_ENROLLMENT_FAILED, teacherEnrollment));

        ImportSummaryLog importSummaryLog = new ImportSummaryLog(enrollments, enrollmentWarnings);

        assertEquals(1, importSummaryLog.summary.enrolledStudents);
        assertEquals(0, importSummaryLog.summary.enrolledTeachers);
        assertEquals(0, importSummaryLog.summary.failedStudents.size());
        assertEquals(1, importSummaryLog.summary.failedTeachers.size());
        assertEquals(1, importSummaryLog.summary.failedTeachers.get(CODE_ENROLLMENT_FAILED.toString()).intValue());
        assertEquals(1, importSummaryLog.enrolledStudents.size());
        assertEquals(0, importSummaryLog.enrolledTeachers.size());
        assertEquals(0, importSummaryLog.failedStudents.size());
        assertEquals(1, importSummaryLog.failedTeachers.size());
        assertEquals(STUDENT_NUMBER, ((StudentEnrollmentEntry) importSummaryLog.enrolledStudents
            .get(0)).studentNumber);
        assertEquals(TEACHER_ID, ((TeacherEnrollmentEntry) importSummaryLog.failedTeachers
            .get(CODE_ENROLLMENT_FAILED.toString()).get(0)).teacherId);

    }
}
