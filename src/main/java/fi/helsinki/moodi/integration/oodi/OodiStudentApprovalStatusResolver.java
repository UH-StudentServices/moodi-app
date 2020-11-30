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

package fi.helsinki.moodi.integration.oodi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@Component
public class OodiStudentApprovalStatusResolver {

    private static List<Integer> enrollmentApprovedStatusCodes;

    @Autowired
    public OodiStudentApprovalStatusResolver(final Environment environment) {
        enrollmentApprovedStatusCodes =
            newArrayList(environment.getRequiredProperty("oodi.enrollmentApprovedStatusCodes", Integer[].class));
    }

    public static boolean isApproved(OodiStudent oodiStudent) {

        if (!oodiStudent.automaticEnabled) {
            return oodiStudent.approved;
        }

        return enrollmentApprovedStatusCodes.contains(oodiStudent.enrollmentStatusCode);
    }
}
