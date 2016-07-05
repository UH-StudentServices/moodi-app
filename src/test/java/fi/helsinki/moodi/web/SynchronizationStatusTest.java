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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SynchronizationStatusTest extends AbstractMoodiIntegrationTest {

    @Test
    public void thatFullSynchronizationFailed() throws Exception {
        mockMvc.perform(
            get("/api/v1/synchronization/status?type=FULL")
                .contentType(MediaType.APPLICATION_JSON)
                .header("client-id", "testclient")
                .header("client-token", "xxx123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Error"))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value(true))
            .andExpect(jsonPath("$.message").value("Last synchronization failed"));
    }

    @Test
    public void thatIncrementalSynchronizationFailed() throws Exception {
        mockMvc.perform(
            get("/api/v1/synchronization/status?type=INCREMENTAL")
                .contentType(MediaType.APPLICATION_JSON)
                .header("client-id", "testclient")
                .header("client-token", "xxx123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("Error"))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value(true))
            .andExpect(jsonPath("$.message").value("Last synchronization failed"));
    }
}
