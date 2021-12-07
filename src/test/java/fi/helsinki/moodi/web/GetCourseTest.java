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

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.http.MediaType;

import static fi.helsinki.moodi.service.course.Course.ImportStatus.COMPLETED_FAILED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GetCourseTest extends AbstractMoodiIntegrationTest {

    private static String REALISATION_ID_NOT_FOUND = "hy-cur-notfound";
    private static String NOT_FOUND_RESPONSE_ERROR = "Course with realisationId %s not found";
    private static String NOT_FOUND_STATUS_STRING = "not-found";

    @Test
    public void thatCourseIsReturned() throws Exception {
        mockMvc.perform(
            get("/api/v1/courses/" + SISU_REALISATION_IN_DB_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("client-id", "testclient")
                .header("client-token", "xxx123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url").value(getMoodleBaseUrl() + "/course/view.php?id=54321"))
            .andExpect(jsonPath("$.importStatus").value(COMPLETED_FAILED.toString()));
    }

    @Test
    public void thatCourseIsNotFound() throws Exception {
        mockMvc.perform(
            get("/api/v1/courses/" + REALISATION_ID_NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .header("client-id", "testclient")
                .header("client-token", "xxx123"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.data").isEmpty())
            .andExpect(jsonPath("$.error").value(String.format(NOT_FOUND_RESPONSE_ERROR, REALISATION_ID_NOT_FOUND)))
            .andExpect(jsonPath("$.status").value(NOT_FOUND_STATUS_STRING));
    }
}
