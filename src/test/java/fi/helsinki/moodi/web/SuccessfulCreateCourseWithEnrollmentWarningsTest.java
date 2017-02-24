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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SuccessfulCreateCourseWithEnrollmentWarningsTest extends AbstractSuccessfulCreateCourseTest {

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