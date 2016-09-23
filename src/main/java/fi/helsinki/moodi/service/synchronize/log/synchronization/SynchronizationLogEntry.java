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

package fi.helsinki.moodi.service.synchronize.log.synchronization;

import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.log.LogEntry;

public class SynchronizationLogEntry implements LogEntry {

    private final SynchronizationSummary synchronizationSummary;
    private final String timestamp;
    private final String title;

    public SynchronizationLogEntry(SynchronizationSummary synchronizationSummary, String title, String timestamp) {
        this.synchronizationSummary = synchronizationSummary;
        this.timestamp = timestamp;
        this.title = title;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public String getTimestamp() {
        return this.timestamp;
    }

    @Override
    public Object getData() {
        return this.synchronizationSummary;
    }
}
