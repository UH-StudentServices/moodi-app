package fi.helsinki.moodi.web;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.http.MediaType;

import static fi.helsinki.moodi.service.course.Course.ImportStatus.COMPLETED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GetCourseTest extends AbstractMoodiIntegrationTest {

    @Test
    public void thatCourseIsReturned() throws Exception {
        mockMvc.perform(
            get("/api/v1/courses/12345")
                .contentType(MediaType.APPLICATION_JSON)
                .header("client-id", "testclient")
                .header("client-token", "xxx123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url").value(getMoodleBaseUrl() + "/course/view.php?id=54321"))
            .andExpect(jsonPath("$.importStatus").value(COMPLETED.toString()));
    }

    @Test
    public void thatCourseIsNotFound() throws Exception {
        mockMvc.perform(
            get("/api/v1/courses/00000")
                .contentType(MediaType.APPLICATION_JSON)
                .header("client-id", "testclient")
                .header("client-token", "xxx123"))
            .andExpect(status().isNotFound());
    }
}
