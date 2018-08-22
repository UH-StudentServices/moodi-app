package fi.helsinki.moodi.integration.iam;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

public class IAMMockClient implements IAMClient {

    private static final Logger LOGGER = getLogger(IAMMockClient.class);

    private final Map<String, String> mockUsers;

    public IAMMockClient(Map<String, String> mockUsers) {
        this.mockUsers = mockUsers;
        LOGGER.info("--- Using mock IAM client! ---");
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
            LOGGER.info("Username {} found for {}", username, key);
            return singletonList(username);
        } else {
            LOGGER.info("Username not found for {}", key);
            return new ArrayList<>();
        }
    }
}
