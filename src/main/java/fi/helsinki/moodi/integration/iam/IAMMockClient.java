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

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

public class IAMMockClient implements IAMClient {

    private static final Logger logger = getLogger(IAMMockClient.class);

    private final Map<String, String> mockUsers;

    public IAMMockClient(Map<String, String> mockUsers) {
        this.mockUsers = mockUsers;
        logger.info("--- Using mock IAM client! ---");
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
            logger.info("Username {} found for {}", username, key);
            return singletonList(username);
        } else {
            logger.info("Username not found for {}", key);
            return new ArrayList<>();
        }
    }
}
