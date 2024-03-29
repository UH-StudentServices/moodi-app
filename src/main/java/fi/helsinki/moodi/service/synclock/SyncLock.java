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

package fi.helsinki.moodi.service.synclock;

import fi.helsinki.moodi.service.course.Course;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_lock")
public class SyncLock {

    @Id
    @SequenceGenerator(name = "sync_lock_id_seq_generator", sequenceName = "sync_lock_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sync_lock_id_seq_generator")
    public Long id;

    @NotBlank
    public String reason;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id")
    public Course course;

    public boolean active = true;

    @Column(name = "created")
    @NotNull
    public LocalDateTime created;

    @Column(name = "modified")
    @NotNull
    public LocalDateTime modified;

}
