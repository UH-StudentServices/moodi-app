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

package fi.helsinki.moodi.integration.moodle;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MoodleClientCreateCourseTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private MoodleClient moodleClient;

    @Test
    public void creatingCourseWithShortnameThatIsAlreadyTaken() {
        moodleMockServer.expect(requestTo(getMoodleRestUrl()))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("wstoken=xxxx1234&wsfunction=core_course_create_courses&moodlewsrestformat=json&courses%5B0%5D%5Bidnumber%5D=12345&courses%5B0%5D%5Bfullname%5D=Fullname&courses%5B0%5D%5Bshortname%5D=Shortname&courses%5B0%5D%5Bcategoryid%5D=1&courses%5B0%5D%5Bsummary%5D=summary&courses%5B0%5D%5Bvisible%5D=0&courses%5B0%5D%5Bcourseformatoptions%5D%5B0%5D%5Bname%5D=numsections&courses%5B0%5D%5Bcourseformatoptions%5D%5B0%5D%5Bvalue%5D=7"))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
                .andRespond(withSuccess(Fixtures.asString("/moodle/create-course-shortname-already-in-use.json"), MediaType.APPLICATION_JSON));

        final MoodleCourse course =
                new MoodleCourse("12345", "Fullname", "Shortname", "1", "summary", false, 7);

        try {
            moodleClient.createCourse(course);
            fail("We want an exception!");
        } catch (MoodleClientException e) {
            assertEquals("Short name is already used for another course (ICT Driv 103127921)", e.getMessage());
            assertEquals("moodle_exception", e.getMoodleException());
            assertEquals("shortnametaken", e.getErrorCode());
        }
    }
}