package fi.helsinki.moodi.service.synchronize.log;

import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import fi.helsinki.moodi.service.synchronize.SynchronizationType;
import fi.helsinki.moodi.service.time.TimeService;
import fi.helsinki.moodi.service.util.JsonUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation that logs each summary to separate file.
 */
@Component
@Profile("!test")
public class FileSynchronizationSummaryLogger implements SynchronizationSummaryLogger {

    private static final Logger LOGGER = getLogger(FileSynchronizationSummaryLogger.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH.mm.ss");

    private final JsonUtil jsonUtil;

    private final File path;
    private final TimeService timeService;
    private final Duration retainLogsDuration;

    @Autowired
    public FileSynchronizationSummaryLogger(Environment environment, TimeService timeService, JsonUtil jsonUtil) {
        this.timeService = timeService;
        this.path = environment.getRequiredProperty("synchronize.logging.file.path", File.class);
        this.jsonUtil = jsonUtil;

        if (!this.path.isDirectory()) {
            this.path.mkdirs();
        }

        final String retainLogsDurationString = environment.getRequiredProperty("synchronize.logging.file.retain.logs");
        this.retainLogsDuration = Duration.parse(retainLogsDurationString);

    }

    @Override
    public void log(final SynchronizationSummary summary) {
        try {
            final File file = new File(getSummaryPath(summary.getType()), makeFilename());
            file.getParentFile().mkdirs();

            jsonUtil.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file, summary);

        } catch (Exception e) {
            throw new RuntimeException("Could not write summary", e);
        }
    }

    @Override
    public void cleanOldLogs() {
        final LocalDateTime date = timeService.getCurrentDateTime().minusSeconds(retainLogsDuration.getSeconds());
        final long timestamp = date.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();

        for (final File summaryDir : getSummaryDirs()) {
            cleanOldLogs(summaryDir, timestamp);
        }
    }

    private void cleanOldLogs(final File summaryDir, final long timestamp) {
        for (final File dir : summaryDir.listFiles(File::isDirectory)) {
            for (File file : dir.listFiles()) {
                if (file.lastModified() < timestamp) {
                    LOGGER.debug("Delete old summary file: {}", file.getAbsolutePath());
                    file.delete();
                }
            }

            if (dir.listFiles().length == 0) {
                LOGGER.debug("Delete empty summary dir: {}", dir.getAbsolutePath());
                dir.delete();
            }
        }
    }

    private String makeFilename() {
        final LocalDateTime now = timeService.getCurrentDateTime();
        final String date = DATE_FORMATTER.format(now);
        final String time = TIME_FORMATTER.format(now);

        return String.format("%s/%s.json", date, time);
    }

    private File getSummaryPath(final SynchronizationType type) {
        return new File(path, type.name());
    }

    private List<File> getSummaryDirs() {
        return Arrays.asList(SynchronizationType.values())
                .stream()
                .map(this::getSummaryPath)
                .filter(File::isDirectory)
                .collect(toList());
    }
}
