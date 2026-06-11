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

package fi.helsinki.moodi.service.synchronize;

/**
 * Describes different types of synchronization runs.
 */
public enum SynchronizationType {
    FULL,
    INCREMENTAL,
    UNLOCK,
    /**
     * Like FULL, but compares against the live Moodle enrollment state instead of the locally
     * stored snapshot, and rewrites the snapshots. Used to bootstrap the snapshots and to
     * correct drift caused by manual changes in Moodle.
     */
    RECONCILE;

    /**
     * Delta runs resolve actions against the locally stored snapshot of the last synced
     * Sisu state and do not fetch the enrollment state from Moodle.
     */
    public boolean isDeltaBased() {
        return this == FULL || this == INCREMENTAL;
    }
}
