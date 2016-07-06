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

package fi.helsinki.moodi.web;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

import static fi.helsinki.moodi.service.course.Course.ImportStatus.COMPLETED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GetCourseAndEnrollmentStatusTest extends AbstractSuccessfulCreateCourseTest {

    @Before
    public void setUp() {
       setUpMockServerResponsesWithWarnings();
    }

    @Test
    public void successfulCreateCourseReturnsCorrectResponse() throws Exception {
        makeCreateCourseRequest(COURSE_REALISATION_ID);

        mockMvc.perform(
            get("/api/v1/courses/"+ COURSE_REALISATION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("client-id", "testclient")
                .header("client-token", "xxx123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url").value(getMoodleBaseUrl() + "/course/view.php?id=988888"))
            .andExpect(jsonPath("$.importStatus").value(COMPLETED.toString()))
            .andExpect(jsonPath("$.courseEnrollmentStatus.studentEnrollments[0].courseEnrollmentStatusCode").value("OK"))
            .andExpect(jsonPath("$.courseEnrollmentStatus.studentEnrollments[0].studentNumber").value("010342729"))
            .andExpect(jsonPath("$.courseEnrollmentStatus.studentEnrollments[1].courseEnrollmentStatusCode").value("OK"))
            .andExpect(jsonPath("$.courseEnrollmentStatus.studentEnrollments[1].studentNumber").value("011119854"))
            .andExpect(jsonPath("$.courseEnrollmentStatus.studentEnrollments[2].courseEnrollmentStatusCode").value("FAILED_NO_MOODLE_USER"))
            .andExpect(jsonPath("$.courseEnrollmentStatus.studentEnrollments[2].studentNumber").value("011524656"))
            .andExpect(jsonPath("$.courseEnrollmentStatus.teacherEnrollments[0].courseEnrollmentStatusCode").value("OK"))
            .andExpect(jsonPath("$.courseEnrollmentStatus.teacherEnrollments[0].teacherId").value("110588"));
    }


}
