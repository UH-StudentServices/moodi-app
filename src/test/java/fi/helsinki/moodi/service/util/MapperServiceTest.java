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

package fi.helsinki.moodi.service.util;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MapperServiceTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private MapperService mapperService;

    @Test
    public void getExistingMoodleRole() {
        assertEquals(getStudentRoleId(), mapperService.getMoodleRole("student"));
        assertEquals(getTeacherRoleId(), mapperService.getMoodleRole("teacher"));
    }

    @Test(expected = IllegalStateException.class)
    public void getNotExistingMoodleRole() {
        mapperService.getMoodleRole("siivooja");
    }

    @Test
    public void getDirectlyMatchingCategory() {
        // "Matlu" directly
        assertEquals("9", mapperService.getMoodleCategoryByOrganisationId("hy-org-1000000911"));
    }

    @Test
    public void getCategoryViaTraversingUpTheOrgTree() {
        expectSisuOrganisationExportRequest(); // Only one request due to caching
        // Plant pathology under "Maa- ja metsä"
        assertEquals("8", mapperService.getMoodleCategoryByOrganisationId("hy-org-34002592"));
    }

    @Test
    public void getDefaultCategoryForUnknownOrg() {
        expectSisuOrganisationExportRequest();
        assertEquals("17", mapperService.getMoodleCategoryByOrganisationId("no-existo"));
    }

    @Test
    public void getDefaultCategoryForNullOrg() {
        assertEquals("17", mapperService.getMoodleCategoryByOrganisationId(null));
    }

    @Test
    public void twoOrganisationExportBatches() {
        studyRegistryMockServer.expect(requestTo(getSisuUrl() + "/kori/api/organisations/v2/export?limit=10000&since=0"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(Fixtures.asString("/sisu/organisation-export-1.json"), MediaType.APPLICATION_JSON));
        studyRegistryMockServer.expect(requestTo(getSisuUrl() + "/kori/api/organisations/v2/export?limit=10000&since=1234"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(Fixtures.asString("/sisu/organisation-export-2.json"), MediaType.APPLICATION_JSON));

        assertEquals("8", mapperService.getMoodleCategoryByOrganisationId("hy-org-34002592"));
    }
}
