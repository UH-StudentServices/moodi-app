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

    private static final Map<String, String> users = ImmutableMap.of(STUDENT_NUMBER, STUDENT_USERNAME, TEACHER_ID, TEACHER_USERNAME);

    private static final IAMClient iamClient = new IAMMockClient(users);

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
