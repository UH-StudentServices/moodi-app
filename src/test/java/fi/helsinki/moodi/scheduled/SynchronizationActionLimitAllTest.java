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
                                  "syncTresholds.REMOVE_ROLE.limit = 10"})
public class SynchronizationActionLimitAllTest extends AbstractSynchronizationJobTest {

    private static final String EXPECTED_REMOVE_ROLE_FROM_ALL_NOT_PERMITTED_MESSAGE = "Action REMOVE_ROLE is not permitted for all items";

    @Test
    public void thatRemovingRolesActionIsLimitedByThreshold() {
        testTresholdCheckFailed(EXPECTED_REMOVE_ROLE_FROM_ALL_NOT_PERMITTED_MESSAGE);
    }
}
