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

package fi.helsinki.moodi.service.synchronize.log.importing;

import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.log.LogEntry;

public class ImportCourseLogEntry implements LogEntry {

    private final Course course;
    private final String title;
    private final String timestamp;

    public ImportCourseLogEntry(Course course, String title, String timestamp) {
        this.course = course;
        this.title = title;
        this.timestamp = timestamp;
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
        return this.course;
    }
}
