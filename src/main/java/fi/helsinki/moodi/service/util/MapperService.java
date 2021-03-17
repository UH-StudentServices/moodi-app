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

import fi.helsinki.moodi.integration.sisu.SisuClient;
import fi.helsinki.moodi.integration.sisu.SisuOrganisation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConfigurationProperties(prefix = "mapper.moodle")
public class MapperService {

    private static final String ROLE_STUDENT = "student";
    private static final String ROLE_TEACHER = "teacher";
    private static final String ROLE_MOODI = "moodi";

    private final Environment environment;
    private final SisuClient sisuClient;

    // These get populated by @ConfigurationProperties
    private String defaultCategory;
    private Map<String, String> moodleCategoriesByOrgId;

    @Autowired
    public MapperService(final Environment environment, SisuClient sisuClient) {
        this.environment = environment;
        this.sisuClient = sisuClient;
    }

    public void setDefaultCategory(String defaultCategory) {
        this.defaultCategory = defaultCategory;
    }

    public String getDefaultCategory() {
        return defaultCategory;
    }

    public Map<String, String> getCategoriesByOrgId() {
        return moodleCategoriesByOrgId;
    }

    public void setCategoriesByOrgId(Map<String, String> moodleCategoriesByOrgId) {
        this.moodleCategoriesByOrgId = moodleCategoriesByOrgId;
    }

    public long getStudentRoleId() {
        return getMoodleRole(ROLE_STUDENT);
    }

    public long getTeacherRoleId() {
        return getMoodleRole(ROLE_TEACHER);
    }

    // Also known as the synced role
    public long getMoodiRoleId() {
        return getMoodleRole(ROLE_MOODI);
    }

    public long getMoodleRole(final String role) {
        return environment.getRequiredProperty("mapper.moodle.role." + role, Long.class);
    }

    public String getMoodleCategoryByOrganisationId(String orgId) {
        if (orgId == null) {
            return defaultCategory;
        }
        String cat = moodleCategoriesByOrgId.get(orgId);
        if (cat != null) {
            return cat;
        } else {
            SisuOrganisation organisation = sisuClient.getAllOrganisationsById().get(orgId);
            return organisation != null ? getMoodleCategoryByOrganisationId(organisation.parentId) : defaultCategory;
        }
    }
}
