package fi.helsinki.moodi.integration.iam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

public class IAMMockClient implements IAMClient {

    private final Map<String, String> mockUsers;

    public IAMMockClient(Map<String, String> mockUsers) {
        this.mockUsers = mockUsers;
    }

    @Override
    public List<String> getStudentUsernameList(String studentNumber) {
        return getUsernameForKey(studentNumber);
    }

    @Override
    public List<String> getTeacherUsernameList(String teacherId) {
        return getUsernameForKey(teacherId);
    }

    private List<String> getUsernameForKey(String key) {
        String username = mockUsers.get(key);

        if (username != null) {
            return singletonList(username);
        } else {
            return new ArrayList<>();
        }
    }
}
