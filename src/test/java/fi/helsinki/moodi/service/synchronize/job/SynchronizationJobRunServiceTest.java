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

package fi.helsinki.moodi.service.synchronize.job;

import fi.helsinki.moodi.service.synchronize.SynchronizationStatus;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SynchronizationJobRunServiceTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private SynchronizationJobRunService synchronizationJobRunService;

    @Autowired
    private SynchronizationJobRunRepository synchronizationJobRunRepository;

    @Test
    public void testIsSynchronizationInProgress() {
        assertFalse(synchronizationJobRunService.isSynchronizationInProgress());
        synchronizationJobRunService.begin(SynchronizationType.FULL);
        assertTrue(synchronizationJobRunService.isSynchronizationInProgress());
    }

    @Test
    public void testCheckInterruptedRuns() {
        synchronizationJobRunService.begin(SynchronizationType.FULL);

        assertEquals(getNumberOfStartedJobs(), 1);
        assertEquals(getNumberOfInterruptedJobs(), 0);

        synchronizationJobRunService.checkForInterruptedRuns();

        assertEquals(getNumberOfStartedJobs(), 0);
        assertEquals(getNumberOfInterruptedJobs(), 1);

    }

    private int getNumberOfStartedJobs() {
        return getNumberOfJobsByStatus(SynchronizationStatus.STARTED);
    }

    private int getNumberOfInterruptedJobs() {
        return getNumberOfJobsByStatus(SynchronizationStatus.INTERRUPTED);
    }

    private int getNumberOfJobsByStatus(SynchronizationStatus status) {
        return synchronizationJobRunRepository
            .findByStatus(status).size();
    }

}
