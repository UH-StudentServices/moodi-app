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

import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synclock.SyncLockService;
import fi.helsinki.moodi.service.synchronize.notify.LockedSynchronizationItemMessageBuilder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@TestPropertySource(properties = {
    "syncTresholds.REMOVE_ENROLLMENT.preventAll = 1",
    "syncTresholds.REMOVE_ENROLLMENT.limit = 10"
})
public class SynchronizationActionLimitAllTest extends AbstractSynchronizationJobTest {

    private static final String EXPECTED_REMOVE_ENROLLMENT_FROM_ALL_NOT_PERMITTED_MESSAGE = "Action REMOVE_ENROLLMENT is not permitted for all items";

    @Autowired
    private SyncLockService syncLockService;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private LockedSynchronizationItemMessageBuilder lockedSynchronizationItemMessageBuilder;

    @Test
    public void thatRemoveEnrollmentActionIsLimitedBypreventAllThreshold() {
        Course course = findCourse();
        assertFalse(syncLockService.isLocked(course));

        testThatThresholdCheckLocksCourse(EXPECTED_REMOVE_ENROLLMENT_FROM_ALL_NOT_PERMITTED_MESSAGE);
    }
}
