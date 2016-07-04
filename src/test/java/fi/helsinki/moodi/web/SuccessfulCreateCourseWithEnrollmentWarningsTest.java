package fi.helsinki.moodi.web;

import org.junit.Before;
import org.junit.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SuccessfulCreateCourseWithEnrollmentWarningsTest extends AbstractSuccessfullCreateCourseTest {

    private static final long COURSE_REALISATION_ID = 102374742L;

    @Before
    public void setUp() {
        setUpMockServerResponsesWithWarnings();
    }

    @Test
    public void successfulCreateCourseReturnsCorrectResponse() throws Exception {
        makeCreateCourseRequest(COURSE_REALISATION_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.moodleCourseId").value(988888));
    }
}