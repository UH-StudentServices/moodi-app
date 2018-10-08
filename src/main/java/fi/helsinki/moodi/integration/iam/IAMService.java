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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IAMService {
    private final IAMClient iamClient;

    public static final String TEACHER_ID_PREFIX = "9";
    public static final String DOMAIN_SUFFIX = "@helsinki.fi";

    @Autowired
    public IAMService(IAMClient iamClient) {
        this.iamClient = iamClient;
    }

    public List<String> getStudentUsernameList(final String studentNumber) {
        List<String> usernameList = iamClient.getStudentUsernameList(studentNumber);

        return usernameList.stream()
                .map(this::appendDomain)
                .collect(Collectors.toList());
    }

    public List<String> getTeacherUsernameList(final String teacherId) {
        final String normalizedTeacherId = TEACHER_ID_PREFIX + teacherId;
        List<String> usernameList = iamClient.getTeacherUsernameList(normalizedTeacherId);

        return usernameList.stream()
                .map(this::appendDomain)
                .collect(Collectors.toList());
    }

    private String appendDomain(final String username) {
        return username + DOMAIN_SUFFIX;
    }
}
