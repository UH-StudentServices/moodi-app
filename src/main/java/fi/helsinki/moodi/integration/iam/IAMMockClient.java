package fi.helsinki.moodi.integration.iam;

import java.util.List;

public class IAMMockClient implements IAMClient {
    @Override
    public List<String> getStudentUsernameList(String studentNumber) {
        return null;
    }

    @Override
    public List<String> getTeacherUsernameList(String teacherId) {
        return null;
    }
}
