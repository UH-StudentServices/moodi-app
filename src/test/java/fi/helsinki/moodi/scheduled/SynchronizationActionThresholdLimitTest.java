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

import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"syncTresholds.REMOVE_ROLE.preventAll = true",
                                  "syncTresholds.REMOVE_ROLE.limit = 1"})

public class SynchronizationActionThresholdLimitTest extends AbstractSynchronizationJobTest {

    private static final String EXPECTED_THRESHOLD_CROSSED_REMOVE_ROLE_MESSAGE = "Action REMOVE_ROLE for 1 items exceeds threshold";

    @Test
    public void thatRemovingRolesActionIsLimitedByThreshold() {
        testTresholdCheckFailed(EXPECTED_THRESHOLD_CROSSED_REMOVE_ROLE_MESSAGE);
    }
}