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

package fi.helsinki.moodi.service.synchronize.log;

import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.courseEnrollment.CourseEnrollmentStatus;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.log.synchronizationSummary.SynchronizationSummaryLog;
import fi.helsinki.moodi.service.time.TimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service that orchestrates logging.
 */
@Service
public class LoggingService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss");

    private static final String SYNCHRONIZATION_SUMMARY_TITLE = "Synchronization run completed:";
    private static final String COURSE_IMPORT_TITLE = "Course import completed:";
    private static final String COURSE_ENROLLMENT_TITLE = "Course import user enrollments completed:";

    private final List<MoodiLogger> loggers;
    private final TimeService timeService;

    @Autowired
    public LoggingService(List<MoodiLogger> loggers, TimeService timeService) {
        this.loggers = loggers;
        this.timeService = timeService;
    }

    public void logSynchronizationSummary(final SynchronizationSummary summary) {
        log(SYNCHRONIZATION_SUMMARY_TITLE, new SynchronizationSummaryLog(summary));
    }

    public void logCourseImport(Course course) {
        log(COURSE_IMPORT_TITLE, course);
    }

    public void logCourseImportEnrollments(CourseEnrollmentStatus courseEnrollmentStatus) {
        log(COURSE_ENROLLMENT_TITLE, courseEnrollmentStatus);
    }

    public void cleanOldLogs() {
        for (final MoodiLogger logger : loggers) {
            logger.cleanOldLogs();
        }
    }

    private void log(String title, Object data) {
        for (final MoodiLogger logger : loggers) {
            logger.log(getCurrentDateTime(), title, data);
        }
    }

    private String getCurrentDateTime() {
        return DATETIME_FORMATTER.format(timeService.getCurrentDateTime());
    }
}
