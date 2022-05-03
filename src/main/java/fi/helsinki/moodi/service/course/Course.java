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

package fi.helsinki.moodi.service.course;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "course")
public class Course {

    public enum ImportStatus {
        IN_PROGRESS,
        COMPLETED,
        COMPLETED_FAILED
    }

    @Id
    @SequenceGenerator(name = "course_id_seq_generator", sequenceName = "course_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "course_id_seq_generator")
    public Long id;

    @Column(name = "realisation_id")
    @NotNull
    public String realisationId;

    @Column(name = "creator_username")
    public String creatorUsername;

    @Column(name = "moodle_id")
    public Long moodleId;

    @Column(name = "created")
    @NotNull
    public LocalDateTime created;

    @Column(name = "modified")
    @NotNull
    public LocalDateTime modified;

    @Column(name = "import_status")
    @NotNull
    @Enumerated(EnumType.STRING)
    public ImportStatus importStatus;

    @Column(name = "removed")
    @NotNull
    public boolean removed;

    @Column(name = "removed_message")
    public String removedMessage;

}
