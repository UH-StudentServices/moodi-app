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

    @Autowired
    private OodiClient oodiClient;

    @Test
    public void deserializeRespose() {
        oodiMockServer.expect(
                requestTo("https://oprek4.it.helsinki.fi:30039/courseunitrealisations/102374742"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(Fixtures.asString("/oodi/course-realisation.json"), MediaType.APPLICATION_JSON));


        final Optional<OodiCourseUnitRealisation> cur = oodiClient.getCourseUnitRealisation(102374742);
        assertOodiCourseUnitRealisation(cur);
    }

    private void assertOodiCourseUnitRealisation(final Optional<OodiCourseUnitRealisation> curOptional) {

        final OodiCourseUnitRealisation cur = curOptional.get();

        assertEquals("590346", cur.baseCode);
        assertEquals("05.12.2014", cur.endDate);
        assertEquals("27.10.2014T235900", cur.enrollmentEndDate);
        assertEquals("01.08.2014T080000", cur.enrollmentStartDate);
        assertEquals("27.06.2014", cur.lastChanged);
        assertEquals("Lapsuus ja yhteiskunta", cur.realisationName.get(0).text);
        assertEquals("27.10.2014", cur.startDate);
        assertEquals(Integer.valueOf(3), cur.creditPoints);
        assertEquals(Integer.valueOf(102374742), cur.realisationId);
        assertEquals(Integer.valueOf(5), cur.realisationTypeCode);

        assertDescriptions(cur);
        assertLanguages(cur);
        assertTeachers(cur);
        assertStudents(cur);
        assertOrganisations(cur);
    }

    private void assertOrganisations(final OodiCourseUnitRealisation cur) {
        assertEquals(1, cur.organisations.size());
        assertOrganisation(cur.organisations.get(0), "H5510", 100);
    }

    private void assertOrganisation(final OodiOrganisation org, final String code, final Integer percentage) {
        assertEquals(code, org.code);
        assertEquals(percentage, org.percentage);
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
                "Kotivuori Tuomas Olavi",
                "Kotivuori",
                "Tuomas",
                "Tuomas Olavi",
                null,
                "110588",
                3);
    }

    private void assertTeacher(
            final OodiTeacher t,
            final String fullName,
            final String lastName,
            final String callingName,
            final String firstNames,
            final String email,
            final String teacherId,
            final Integer teacherRoleCode) {

        assertEquals(fullName, t.fullName);
        assertEquals(lastName, t.lastName);
        assertEquals(callingName, t.callingName);
        assertEquals(firstNames, t.firstNames);
        assertEquals(email, t.email);
        assertEquals(teacherId, t.teacherId);
        assertEquals(teacherRoleCode, t.teacherRoleCode);
    }

    private void assertStudents(final OodiCourseUnitRealisation cur) {
        assertEquals(3, cur.students.size());
        assertStudent(cur.students.get(0), "040-1234321", null, "Niina Johanna", "Sulin", "010342729", 3);
        assertStudent(cur.students.get(1), "040-1234321", null, "Milla Maaret", "Uromo", "011119854", 3);
        assertStudent(cur.students.get(2), "040-1234321", null, "Virpi Tuulikki", "Kontinen", "011524656", 3);
    }

    private void assertStudent(
            final OodiStudent s,
            final String mobilePhone,
            final String email,
            final String firstNames,
            final String lastName,
            final String studentNumber,
            final Integer enrollmentStatusCode) {

        assertEquals(mobilePhone, s.mobilePhone);
        assertEquals(email, s.email);
        assertEquals(firstNames, s.firstNames);
        assertEquals(lastName, s.lastName);
        assertEquals(studentNumber, s.studentNumber);
        assertEquals(enrollmentStatusCode, s.enrollmentStatusCode);
    }
}
