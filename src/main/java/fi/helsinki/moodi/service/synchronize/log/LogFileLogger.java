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

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.helsinki.moodi.service.util.JsonUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation that logs to log file.
 */
@Component
@Profile({"test", "local"})
public class LogFileLogger implements MoodiLogger {

    private final JsonUtil jsonUtil;

    private static final Logger LOGGER = getLogger(LogFileLogger.class);

    @Autowired
    public LogFileLogger(JsonUtil jsonUtil) {
        this.jsonUtil = jsonUtil;
    }

    @Override
    public void log(String timestamp, String title, Object data) {
        try {
            final String jsonData = jsonUtil
                .getObjectMapper()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(data);
            LOGGER.info(title);
            LOGGER.info(jsonData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not write summary", e);
        }
    }

    @Override
    public void cleanOldLogs() {
        // Do nothing
    }
}
