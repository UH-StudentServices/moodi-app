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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;

public class MapperServiceTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private MapperService mapperService;

    @Test
    public void getDefaultMoodleCategory() throws Exception {
        assertEquals("73", mapperService.getDefaultCategory());
    }

    @Test
    public void getExistingMoodleRole() throws Exception {
        assertEquals(getStudentRoleId(), mapperService.getMoodleRole("student"));
        assertEquals(getTeacherRoleId(), mapperService.getMoodleRole("teacher"));
    }

    @Test(expected = IllegalStateException.class)
    public void getNotExistingMoodleRole() throws Exception {
        mapperService.getMoodleRole("siivooja");
    }
}
