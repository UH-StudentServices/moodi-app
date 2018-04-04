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

package fi.helsinki.moodi.service.synchronize.job;

import javax.persistence.*;

import fi.helsinki.moodi.service.synchronize.SynchronizationStatus;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "synchronization_job_run")
public class SynchronizationJobRun implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "synchronization_job_run_id_seq_generator", sequenceName = "synchronization_job_run_id_seq")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "synchronization_job_run_id_seq_generator")
    public Long id;

    @Column(name = "message")
    @NotNull
    public String message;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @NotNull
    public SynchronizationStatus status;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @NotNull
    public SynchronizationType type;

    @Column(name = "started")
    @NotNull
    public LocalDateTime started;

    @Column(name = "completed")
    public LocalDateTime completed;
}
