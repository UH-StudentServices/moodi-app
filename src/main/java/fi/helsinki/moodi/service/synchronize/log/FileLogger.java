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

import fi.helsinki.moodi.service.time.TimeService;
import fi.helsinki.moodi.service.util.JsonUtil;
import fi.helsinki.moodi.service.util.JsonViews;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation that logs to separate files.
 */
@Component
public class FileLogger implements MoodiLogger {
    private static final Logger LOGGER = getLogger(FileLogger.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    protected final File path;
    private final JsonUtil jsonUtil;
    private final TimeService timeService;
    private final Duration retainLogsDuration;

    @Autowired
    public FileLogger(Environment environment, TimeService timeService, JsonUtil jsonUtil) {
        this.timeService = timeService;
        this.jsonUtil = jsonUtil;
        this.path = environment.getRequiredProperty("logging.file-logging-path", File.class);
        final String retainLogsDurationString = environment.getRequiredProperty("logging.retain-logs");
        this.retainLogsDuration = Duration.parse(retainLogsDurationString);

        if (!this.path.isDirectory()) {
            this.path.mkdirs();
        }
    }

    @Override
    public void log(LogEntry logEntry) {

        try {
            final File file = new File(this.path, getFileName());
            file.getParentFile().mkdirs();

            Files.write(
                Paths.get(file.getPath()),
                Arrays.asList(
                    logEntry.getTimestamp() + ": " + logEntry.getTitle(),
                    jsonUtil.getObjectMapper()
                        .writerWithDefaultPrettyPrinter()
                        .withView(JsonViews.FileLogging.class)
                        .writeValueAsString(logEntry.getData())),
                Charset.forName("UTF-8"),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);

        } catch (Exception e) {
            throw new RuntimeException("Could not write log entry", e);
        }
    }


    public void cleanOldLogs() {
        final LocalDateTime date = timeService.getCurrentDateTime().minusSeconds(retainLogsDuration.getSeconds());
        final long timestamp = date.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();

        cleanOldLogs(path, timestamp);
    }

    private void cleanOldLogs(final File rootDirectory, final long timestamp) {
        for (final File file : rootDirectory.listFiles()) {

            if(file.isDirectory()) {
                cleanOldLogs(file, timestamp);
            } else if (file.lastModified() < timestamp) {
                LOGGER.debug("Delete old summary file: {}", file.getAbsolutePath());
                file.delete();
            }
        }

        if(rootDirectory.listFiles().length == 0) {
            LOGGER.debug("Delete empty summary dir: {}", rootDirectory.getAbsolutePath());
            rootDirectory.delete();
        }
    }

    private String getFileName() {
        final LocalDateTime now = timeService.getCurrentDateTime();
        final String date = DATE_FORMATTER.format(now);
        return String.format("%s.json", date);
    }

}
