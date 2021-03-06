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

package fi.helsinki.moodi.integration.moodle;

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class MoodleServiceGetUserTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private MoodleClient moodleClient;

    @Autowired
    private MoodleService moodleService;

    @Test
    public void noUsername() {
        final Optional<MoodleUser> userList = moodleService.getUser(null);

        assertEquals(userList.isPresent(), false);
    }

}
