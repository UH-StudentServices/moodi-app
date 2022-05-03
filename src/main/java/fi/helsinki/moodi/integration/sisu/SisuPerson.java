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

package fi.helsinki.moodi.integration.sisu;

import fi.helsinki.moodi.integration.studyregistry.StudyRegistryStudent;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryTeacher;
import io.aexp.nodes.graphql.annotations.GraphQLArgument;
import io.aexp.nodes.graphql.annotations.GraphQLProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SisuPerson {
    public String id;
    public String studentNumber;
    public String lastName;
    public String firstNames;
    /**
     * This may be null, for example for Open university Students that have never activated their
     * University of Helsinki user accounts. In that case, they would not be found by student number from IAM either.
     */
    public String eduPersonPrincipalName;
    public String employeeNumber;

    public SisuPerson() {
    }

    public SisuPerson(String studentNumber, String firstNames, String lastName) {
        this.studentNumber = studentNumber;
        this.firstNames = firstNames;
        this.lastName = lastName;
    }

    public StudyRegistryStudent toStudyRegistryStudent(boolean isEnrolled) {
        StudyRegistryStudent ret = new StudyRegistryStudent();
        ret.firstNames = firstNames;
        ret.lastName = lastName;
        ret.studentNumber = studentNumber;
        ret.userName = eduPersonPrincipalName;
        ret.isEnrolled = isEnrolled;
        return ret;
    }

    public StudyRegistryTeacher toStudyRegistryTeacher() {
        StudyRegistryTeacher ret = new StudyRegistryTeacher();
        ret.firstNames = firstNames;
        ret.lastName = lastName;
        ret.employeeNumber = employeeNumber;
        ret.userName = eduPersonPrincipalName;
        return ret;
    }

    public static List<StudyRegistryTeacher> toStudyRegistryTeachers(List<SisuPerson> sisuPeople) {
        return sisuPeople != null ?
            sisuPeople.stream().map(SisuPerson::toStudyRegistryTeacher).collect(Collectors.toList()) : new ArrayList<>();
    }

    public static class SisuPersonWrapper {
        @GraphQLProperty(name = "private_persons", arguments = {
            @GraphQLArgument(name = "ids", type = "String")
        })
        @SuppressWarnings("checkstyle:MemberName")
        public List<SisuPerson> private_persons;
    }
}
