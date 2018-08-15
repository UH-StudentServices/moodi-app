package fi.helsinki.moodi.integration.iam;

import java.util.List;

public interface IAMClient {
    List<String> getStudentUsernameList(String studentNumber);
    List<String> getTeacherUsernameList(String teacherId);
}
