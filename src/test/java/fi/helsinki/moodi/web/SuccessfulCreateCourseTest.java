package fi.helsinki.moodi.web;

import org.junit.Before;
import org.junit.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SuccessfulCreateCourseTest extends AbstractSuccessfullCreateCourseTest {

    private static final long COURSE_REALISATION_ID = 102374742L;

    @Before
    public void setUp() {
        setUpMockServerResponses();
    }

    @Test
    public void successfulCreateCourseReturnsCorrectResponse() throws Exception {
        makeCreateCourseRequest(COURSE_REALISATION_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.moodleCourseId").value(988888));
    }

    @Test
    public void successfulCreateCourseInvokesCorrectIntegrationServices() throws Exception {
        makeCreateCourseRequest(COURSE_REALISATION_ID).andReturn();
        oodiMockServer.verify();
        moodleMockServer.verify();
        esbMockServer.verify();
    }
}