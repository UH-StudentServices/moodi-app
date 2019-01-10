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
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.notify.LockedSynchronizationItemMessageBuilder;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.test.context.TestPropertySource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@TestPropertySource(properties = {"syncTresholds.REMOVE_ROLES.preventAll = 1",
                                  "syncTresholds.REMOVE_ROLES.limit = 1"})
public class SynchronizationActionThresholdLimitTest extends AbstractSynchronizationJobTest {

    private static final String EXPECTED_THRESHOLD_CROSSED_REMOVE_ROLE_MESSAGE = "Action REMOVE_ROLES for 1 items exceeds threshold";

    @Autowired
    private SyncLockService syncLockService;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private LockedSynchronizationItemMessageBuilder lockedSynchronizationItemMessageBuilder;

    @Test
    public void thatRemovingRolesActionIsLimitedByThreshold() {
        Course course = findCourse();
        assertFalse(syncLockService.isLocked(course));

        SynchronizationSummary summary = testTresholdCheckFailed(EXPECTED_THRESHOLD_CROSSED_REMOVE_ROLE_MESSAGE);
        Mockito.verify(mailSender).send(lockedSynchronizationItemMessageBuilder.buildMessage(summary.getItems()));

        assertTrue(syncLockService.isLocked(course));
    }
}
