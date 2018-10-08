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

package fi.helsinki.moodi.integration.iam;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class IAMMockClientTest {

    private static final String STUDENT_NUMBER = "12345";
    private static final String STUDENT_USERNAME = "studentUsername";

    private static final String TEACHER_ID = "54321";
    private static final String TEACHER_USERNAME = "teacherUsername";

    private static final Map<String, String> USERS = ImmutableMap.of(STUDENT_NUMBER, STUDENT_USERNAME, TEACHER_ID, TEACHER_USERNAME);

    private static final IAMClient iamClient = new IAMMockClient(USERS);

    @Test
    public void thatStudentUsernameIsFound() {
        assertUsernameResult(STUDENT_USERNAME, iamClient.getStudentUsernameList(STUDENT_NUMBER));
    }

    @Test
    public void thatTeacherUsernameIsFound() {
        assertUsernameResult(TEACHER_USERNAME, iamClient.getTeacherUsernameList(TEACHER_ID));
    }

    private void assertUsernameResult(String expectedUsername, List<String> result) {
        assertEquals(1, result.size());
        assertEquals(expectedUsername, result.get(0));
    }

    @Test
    public void thatEmptyListIsReturnedIfUsernameIsNotFound() {
        assertEquals(0, iamClient.getStudentUsernameList("1").size());
    }
}
