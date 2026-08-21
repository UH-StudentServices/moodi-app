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

import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReconcileCourseTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private CourseService courseService;

    @Test
    public void thatMarkingCourseForReconcileClearsTheBootstrapFlag() throws Exception {
        courseService.markSyncBootstrapped(SISU_REALISATION_IN_DB_ID);
        assertTrue(courseService.findByRealisationId(SISU_REALISATION_IN_DB_ID).get().syncBootstrapped);

        mockMvc.perform(
            post("/api/v1/synchronization/reconcile/" + SISU_REALISATION_IN_DB_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("client-id", "testclient")
                .header("client-token", "xxx123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("Course marked for reconcile on the next scheduled synchronization run"));

        assertFalse(courseService.findByRealisationId(SISU_REALISATION_IN_DB_ID).get().syncBootstrapped);
    }

    @Test
    public void thatMarkingUnknownCourseForReconcileReturnsNotFound() throws Exception {
        mockMvc.perform(
            post("/api/v1/synchronization/reconcile/hy-cur-does-not-exist")
                .contentType(MediaType.APPLICATION_JSON)
                .header("client-id", "testclient")
                .header("client-token", "xxx123"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Study registry course not found with realisation id hy-cur-does-not-exist"));
    }
}
