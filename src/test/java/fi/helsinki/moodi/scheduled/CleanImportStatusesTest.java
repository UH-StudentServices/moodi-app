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

package fi.helsinki.moodi.scheduled;

import com.google.common.collect.Lists;
import fi.helsinki.moodi.service.course.CourseRepository;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static fi.helsinki.moodi.service.course.Course.ImportStatus;
import static fi.helsinki.moodi.service.course.Course.ImportStatus.COMPLETED_FAILED;
import static fi.helsinki.moodi.service.course.Course.ImportStatus.IN_PROGRESS;
import static org.junit.Assert.assertEquals;

public class CleanImportStatusesTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private CleanImportStatuses cleanImportStatuses;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    public void thatImportStatusesAreCleaned() {

        assertCourseCountByStatus(IN_PROGRESS, 2);
        assertCourseCountByStatus(COMPLETED_FAILED, 0);

        cleanImportStatuses.execute();

        assertCourseCountByStatus(IN_PROGRESS, 1);
        assertCourseCountByStatus(COMPLETED_FAILED, 1);
    }

    private void assertCourseCountByStatus(ImportStatus status, int expextedCount) {
        assertEquals(courseRepository.findByImportStatusIn(Lists.newArrayList(status)).size(), expextedCount);
    }
}
