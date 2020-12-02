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

    public List<String> getStudentUserNameList(final String studentNumber) {
        List<String> userNameList = iamClient.getStudentUserNameList(studentNumber);

        return userNameList.stream()
                .map(this::appendDomain)
                .collect(Collectors.toList());
    }

    public String getStudentUserName(final String studentNumber) {
        List<String> usernameList = getStudentUserNameList(studentNumber);
        return usernameList.isEmpty() ? "not_found_in_iam" : usernameList.get(0);
    }

    public List<String> getTeacherUserNameList(final String employeeNumber) {
        List<String> userNameList = iamClient.getTeacherUserNameList(getPrefixedEmployeeNumber(employeeNumber));

        return userNameList.stream()
                .map(this::appendDomain)
                .collect(Collectors.toList());
    }

    private String getPrefixedEmployeeNumber(final String employeeNumber) {
        if (employeeNumber.startsWith(TEACHER_ID_PREFIX)) {
            return employeeNumber;
        }

        return TEACHER_ID_PREFIX + employeeNumber;
    }

    private String appendDomain(final String username) {
        return username + DOMAIN_SUFFIX;
    }
}
