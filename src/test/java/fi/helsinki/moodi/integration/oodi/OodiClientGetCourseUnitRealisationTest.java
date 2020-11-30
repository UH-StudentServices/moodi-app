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

package fi.helsinki.moodi.integration.oodi;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class OodiClientGetCourseUnitRealisationTest extends AbstractMoodiIntegrationTest {

    private static final long REALISATION_ID = 102374742;

    @Autowired
    private OodiClient oodiClient;

    @Test
    public void deserializeResponse() {
        oodiMockServer.expect(
                requestTo(getOodiCourseUnitRealisationRequestUrl("102374742")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(Fixtures.asString("/oodi/course-realisation.json"), MediaType.APPLICATION_JSON));

        final Optional<OodiCourseUnitRealisation> cur = oodiClient.getCourseUnitRealisation("102374742");
        assertOodiCourseUnitRealisation(cur);
    }

    private void assertOodiCourseUnitRealisation(final Optional<OodiCourseUnitRealisation> curOptional) {

        final OodiCourseUnitRealisation cur = curOptional.get();

        assertEquals("2019-11-04T22:00:00.000Z", cur.endDate);
        assertEquals("Lapsuus ja yhteiskunta", cur.realisationName.get(0).text);
        assertEquals(Integer.valueOf(102374742), cur.realisationId);

        assertDescriptions(cur);
        assertLanguages(cur);
        assertTeachers(cur);
        assertStudents(cur);
    }

    private void assertDescriptions(final OodiCourseUnitRealisation cur) {
        assertEquals(2, cur.descriptions.size());
        assertEquals(cur.descriptions.get(0).id, Integer.valueOf(22));
        assertEquals("Description 1 (fi)", cur.descriptions.get(0).texts.get(0).text);
        assertEquals("Description 1 (sv)", cur.descriptions.get(0).texts.get(1).text);
        assertEquals("Description 1 (en)", cur.descriptions.get(0).texts.get(2).text);
        assertEquals(cur.descriptions.get(1).id, Integer.valueOf(26));
        assertEquals("Description 2 (fi)", cur.descriptions.get(1).texts.get(0).text);
        assertEquals("Description 2 (sv)", cur.descriptions.get(1).texts.get(1).text);
        assertEquals("Description 2 (en)", cur.descriptions.get(1).texts.get(2).text);
    }

    private void assertLanguages(final OodiCourseUnitRealisation cur) {
        assertEquals(1, cur.languages.size());
        assertLanguage(cur.languages.get(0), "en");
    }

    private void assertLanguage(final OodiLanguage l, final String langCode) {
        assertEquals(langCode, langCode);
    }

    private void assertTeachers(final OodiCourseUnitRealisation cur) {
        assertEquals(1, cur.teachers.size());
        assertTeacher(cur.teachers.get(0),
                "110588");
    }

    private void assertTeacher(
            final OodiTeacher t,
            final String employeeNumber) {

        assertEquals(employeeNumber, t.employeeNumber);
    }

    private void assertStudents(final OodiCourseUnitRealisation cur) {
        assertEquals(4, cur.students.size());
        assertStudent(cur.students.get(0), "010342729");
        assertStudent(cur.students.get(1), "011119854");
        assertStudent(cur.students.get(2), "011524656");
        assertStudent(cur.students.get(3), "011524658");
    }

    private void assertStudent(
            final OodiStudent s,
            final String studentNumber) {
        assertEquals(studentNumber, s.studentNumber);
    }
}
