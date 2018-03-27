package fi.helsinki.moodi.integration.oodi;


import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OodiStudentApprovalStatusResolverTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private OodiStudentApprovalStatusResolver oodiStudentApprovalStatusResolver;

    @Test
    public void thatStudentIsNotApprovedIfNotAutomaticEnabledAndNotApproved() {
        assertFalse(oodiStudentApprovalStatusResolver.isApproved(getOodiStudent(false, false)));
    }

    @Test
    public void thatStudentIsApprovedIfNotAutomaticEnabledAndNotApproved() {
        assertTrue(oodiStudentApprovalStatusResolver.isApproved(getOodiStudent(true, false)));
    }

    @Test
    public void thatStudentIsNotApprovedIfAutomaticEnabledAndNonApprovedStatusCode() {
        assertFalse(oodiStudentApprovalStatusResolver.isApproved(getOodiStudent(true, true, NON_APPROVED_ENROLLMENT_STATUS_CODE)));
    }

    @Test
    public void thatStudentIsApprovedIfAutomaticEnabledAndApprovedStatusCode() {
        assertTrue(oodiStudentApprovalStatusResolver.isApproved(getOodiStudent(true, true)));
    }

    private OodiStudent getOodiStudent(boolean approved, boolean automaticEnabled) {
        return getOodiStudent(approved, automaticEnabled, APPROVED_ENROLLMENT_STATUS_CODE);
    }

    private OodiStudent getOodiStudent(boolean approved, boolean automaticEnabled, int enrollmentStatusCode) {
        OodiStudent oodiStudent = new OodiStudent();
        oodiStudent.automaticEnabled = automaticEnabled;
        oodiStudent.approved = approved;
        oodiStudent.enrollmentStatusCode = enrollmentStatusCode;
        return oodiStudent;
    }
}
