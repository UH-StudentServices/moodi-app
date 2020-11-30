package fi.helsinki.moodi.integration.sisu;

public class SisuEnrolment {

    public EnrolmentState state;
    public SisuPerson person;

    public SisuEnrolment() {}

    public SisuEnrolment(EnrolmentState state, SisuPerson person) {
        this.state = state;
        this.person = person;
    }

    public boolean isEnrolled() {
        return EnrolmentState.ENROLLED.equals(state) || EnrolmentState.CONFIRMED.equals(state);
    }

    public enum EnrolmentState {
        NOT_ENROLLED, PROCESSING, RESERVED,
        CONFIRMED, ENROLLED, REJECTED,
        ABORTED_BY_STUDENT, ABORTED_BY_TEACHER
    }
}
