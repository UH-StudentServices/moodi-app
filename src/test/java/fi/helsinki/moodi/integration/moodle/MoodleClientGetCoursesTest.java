package fi.helsinki.moodi.integration.moodle;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MoodleClientGetCoursesTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private MoodleClient moodleClient;

    @Test
    public void deserializeRespose() {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("wstoken=xxxx1234&wsfunction=core_course_get_courses&moodlewsrestformat=json"))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
                .andRespond(withSuccess(Fixtures.asString("/moodle/get-courses.json"), MediaType.APPLICATION_JSON));

        final List<MoodleFullCourse> courses = moodleClient.getCourses(new ArrayList<>());
        assertEquals(36, courses.size());

        final MoodleFullCourse course = courses.get(0);
        assertEquals(new Long(9), course.id);
        assertEquals("LYH", course.shortName);
        assertEquals(new Integer(1), course.categoryId);
    }
}
