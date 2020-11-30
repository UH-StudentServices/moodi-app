package fi.helsinki.moodi.integration.sisu;

import fi.helsinki.moodi.integration.studyregistry.StudyRegistryStudent;
import fi.helsinki.moodi.integration.studyregistry.StudyRegistryTeacher;
import io.aexp.nodes.graphql.annotations.GraphQLArgument;
import io.aexp.nodes.graphql.annotations.GraphQLProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SisuPerson {
    public String studentNumber;
    public String lastName;
    public String firstNames;
    public String eduPersonPrincipalName;
    public String employeeNumber;

    public SisuPerson() {}

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

    public static List<StudyRegistryTeacher> toStudyRegistryTeachers(List<SisuPerson> sisuPeople ) {
        return sisuPeople != null ?
            sisuPeople.stream().map(SisuPerson::toStudyRegistryTeacher).collect(Collectors.toList()) : new ArrayList<>();
    }

    public static class SisuPersonWrapper {
        @GraphQLProperty(name = "private_persons", arguments = {
            @GraphQLArgument(name = "ids", type = "String")
        })
        public List<SisuPerson> private_persons;
    }
}
