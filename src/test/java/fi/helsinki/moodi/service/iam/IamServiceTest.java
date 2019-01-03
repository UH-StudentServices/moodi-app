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

package fi.helsinki.moodi.service.iam;

import fi.helsinki.moodi.integration.iam.IAMClient;
import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Before;
import org.junit.Test;

import static fi.helsinki.moodi.service.iam.IAMService.TEACHER_ID_PREFIX;
import static org.mockito.Mockito.*;

public class IamServiceTest extends AbstractMoodiIntegrationTest {

    private static final String TEACHER_ID = "0123456";
    private static final String PREFIXED_TEACHER_ID = TEACHER_ID_PREFIX + TEACHER_ID;

    private IAMService iamService;

    private IAMClient mockIamClient;

    @Before
    public void setup() {
        mockIamClient = mock(IAMClient.class);
        iamService = new IAMService(mockIamClient);
    }

    @Test
    public void thatTeacherIdPrefexIsAddedWhenMissing() {
        iamService.getTeacherUsernameList(TEACHER_ID);

        verify(mockIamClient, times(1)).getTeacherUsernameList(PREFIXED_TEACHER_ID);
    }

    @Test
    public void thatTeacherIdPrefixIsNotAddedIfAlreadyExists() {
        iamService.getTeacherUsernameList(PREFIXED_TEACHER_ID);

        verify(mockIamClient, times(1)).getTeacherUsernameList(PREFIXED_TEACHER_ID);
    }
}
