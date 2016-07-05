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

package fi.helsinki.moodi.service.importing;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.Optional;

public final class Enrollment implements Serializable {

    public static final String ROLE_TEACHER = "teacher";
    public static final String ROLE_STUDENT = "student";

    private static final long serialVersionUID = 1L;

    public String role;
    public Optional<String> teacherId;
    public Optional<String> studentNumber;
    public Optional<Long> moodleId;
    public Optional<String> username;

    public static Enrollment forStudent(final String studentNumber) {
        return new Enrollment(ROLE_STUDENT, Optional.empty(), Optional.of(studentNumber), Optional.empty(), Optional.empty());
    }

    public static Enrollment forTeacher(final String teacherId) {
        return new Enrollment(ROLE_TEACHER, Optional.of(teacherId), Optional.empty(), Optional.empty(), Optional.empty());
    }

    private Enrollment(String role, Optional<String> teacherId, Optional<String> studentNumber, Optional<String> username, Optional<Long> moodleId) {
        this.role = role;
        this.teacherId = teacherId;
        this.studentNumber = studentNumber;
        this.username = username;
        this.moodleId = moodleId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
