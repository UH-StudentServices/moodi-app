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

import java.util.List;

public class SisuEnrolment {

    public EnrolmentState state;
    public SisuPerson person;
    public List<String> confirmedStudySubGroupIds;

    public SisuEnrolment() {
    }

    public SisuEnrolment(EnrolmentState state, SisuPerson person, List<String> confirmedStudySubGroupIds) {
        this.state = state;
        this.person = person;
        this.confirmedStudySubGroupIds = confirmedStudySubGroupIds;
    }

    public boolean isEnrolled() {
        return EnrolmentState.ENROLLED.equals(state);
    }

    public enum EnrolmentState {
        NOT_ENROLLED, PROCESSING, RESERVED,
        CONFIRMED, ENROLLED, REJECTED,
        ABORTED_BY_STUDENT, ABORTED_BY_TEACHER
    }
}
