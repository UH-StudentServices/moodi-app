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

import fi.helsinki.moodi.test.AbstractMoodiIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OodiStudentApprovalStatusResolverTest extends AbstractMoodiIntegrationTest {

    @Autowired
    private OodiStudentApprovalStatusResolver oodiStudentApprovalStatusResolver;

    @Test
    public void thatStatusIsResolvedToApprovedWhenApprovedAndAutomaticIsNotEnabled() {
        assertTrue(oodiStudentApprovalStatusResolver.isApproved(getOodiStudent(true, false)));
    }

    @Test
    public void thatStatusIsResolvedToNotApprovedWhenNotApprovedAndAutomaticIsNotEnabled() {
        assertFalse(oodiStudentApprovalStatusResolver.isApproved(getOodiStudent(false, false)));
    }

    @Test
    public void thatStatusIsResolvedToApprovedWhenAutomaticIsEnabledAndStatusCodeIsApproved() {
        assertTrue(oodiStudentApprovalStatusResolver.isApproved(getOodiStudent(true, true)));
    }

    @Test
    public void thatStatusIsResolvedToNotApprovedWhenAutomaticIsEnabledAndStatusCodeIsNotApproved() {
        assertFalse(oodiStudentApprovalStatusResolver.isApproved(getOodiStudent(true, true, NON_APPROVED_ENROLLMENT_STATUS_CODE)));
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
