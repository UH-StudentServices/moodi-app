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

package fi.helsinki.moodi.service.synchronize.snapshot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * The last Sisu-derived participant state that was successfully applied to Moodle
 * for one course + user. Used to compute deltas in synchronization without
 * fetching the enrollment state from Moodle.
 */
@Entity
@Table(name = "course_sync_participant")
public class CourseSyncParticipant {

    @Id
    @SequenceGenerator(name = "course_sync_participant_id_seq_generator",
        sequenceName = "course_sync_participant_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_sync_participant_id_seq_generator")
    public Long id;

    @Column(name = "course_id")
    @NotNull
    public Long courseId;

    @NotBlank
    public String username;

    @Column(name = "moodle_user_id")
    public Long moodleUserId;

    @Column(name = "is_student")
    public boolean student;

    @Column(name = "is_enrolled_student")
    public boolean enrolledStudent;

    @Column(name = "is_teacher")
    public boolean teacher;

    @Column(name = "created")
    @NotNull
    public LocalDateTime created;

    @Column(name = "modified")
    @NotNull
    public LocalDateTime modified;

    public CourseSyncParticipant() {
    }

    public CourseSyncParticipant(Long courseId, String username, Long moodleUserId,
                                 boolean student, boolean enrolledStudent, boolean teacher) {
        this.courseId = courseId;
        this.username = username;
        this.moodleUserId = moodleUserId;
        this.student = student;
        this.enrolledStudent = enrolledStudent;
        this.teacher = teacher;
    }

    public boolean hasSameStateAs(CourseSyncParticipant other) {
        return other != null
            && student == other.student
            && enrolledStudent == other.enrolledStudent
            && teacher == other.teacher;
    }
}
