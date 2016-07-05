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

import com.google.common.collect.Lists;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class MapperServiceTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private MapperService mapperService;

    @Test
    public void getExistingMoodleCategory() throws Exception {
        assertEquals("12", mapperService.getMoodleCategory("H70"));
    }

    @Test
    public void getExistingMoodleCategoryWithWildcard() throws Exception {
        assertEquals("11", mapperService.getMoodleCategory("H1234"));
    }

    @Test
    public void ifMappingIsNotFoundDefaultMoodleCategoryIsUsed() throws Exception {
        assertEquals("17", mapperService.getMoodleCategory("xxxx"));
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

    @Test
    public void sortOrganisationMathcerPatterns() {
        final List<String> patterns = Lists.newArrayList(
                "H1*",
                "H2*",
                "H3*",
                "H4*",
                "H50*",
                "H51*",
                "H52*",
                "H55*",
                "H57*",
                "H6*",
                "H70",
                "H72*",
                "H74*",
                "H799",
                "H8*",
                "H90",
                "H901*",
                "H902*",
                "H906",
                "H91*",
                "H92",
                "H920",
                "H921",
                "H922",
                "H923",
                "H929",
                "H93*",
                "H941",
                "H955",
                "H985*",
                "H99");

        final List<String> expected = Lists.newArrayList(
                "H799",
                "H906",
                "H920",
                "H921",
                "H922",
                "H923",
                "H929",
                "H941",
                "H955",
                "H70",
                "H90",
                "H92",
                "H99",
                "H901*",
                "H902*",
                "H985*",
                "H50*",
                "H51*",
                "H52*",
                "H55*",
                "H57*",
                "H72*",
                "H74*",
                "H91*",
                "H93*",
                "H1*",
                "H2*",
                "H3*",
                "H4*",
                "H6*",
                "H8*"
        );

        patterns.sort(new MapperService.OrganisationMatcherPatternComparator());

        for (int i = 0; i < patterns.size(); i++) {
            assertEquals(
                    String.format("Pattern '%s' in wrong index %s", patterns.get(i), i),
                    expected.get(i),
                    patterns.get(i));
        }

    }
}
