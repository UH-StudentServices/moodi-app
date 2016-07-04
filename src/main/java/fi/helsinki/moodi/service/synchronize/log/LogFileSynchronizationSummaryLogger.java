package fi.helsinki.moodi.service.synchronize.log;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import fi.helsinki.moodi.service.synchronize.SynchronizationSummary;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation that logs summary to log file.
 */
@Component
public class LogFileSynchronizationSummaryLogger implements SynchronizationSummaryLogger {

    private static final Logger LOGGER = getLogger(LogFileSynchronizationSummaryLogger.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .registerModule(new ParameterNamesModule())
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
            .setSerializationInclusion(JsonInclude.Include.ALWAYS)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public void log(final SynchronizationSummary summary) {
        try {
            final String output = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary);
            LOGGER.info(output);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not write summary", e);
        }
    }

    @Override
    public void cleanOldLogs() {
        // Do nothing
    }
}
