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
