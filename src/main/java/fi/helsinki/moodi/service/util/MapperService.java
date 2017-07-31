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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class MapperService {

    private static final String ROLE_STUDENT = "student";
    private static final String ROLE_TEACHER = "teacher";
    private static final String ROLE_MOODI = "moodi";

    private final Environment environment;

    @Value("${mapper.moodle.defaultCategory}")
    private String defaultCategory;

    @Autowired
    public MapperService(final Environment environment) {
        this.environment = environment;
    }

    public void setDefaultCategory(String defaultCategory) {
        this.defaultCategory = defaultCategory;
    }

    public String getDefaultCategory() {
        return defaultCategory;
    }

    public long getStudentRoleId() {
        return getMoodleRole(ROLE_STUDENT);
    }

    public long getTeacherRoleId() {
        return getMoodleRole(ROLE_TEACHER);
    }

    public long getMoodiRoleId() {
        return getMoodleRole(ROLE_MOODI);
    }

    public long getMoodleRole(final String role) {
        return environment.getRequiredProperty("mapper.moodle.role." + role, Long.class);
    }
}
