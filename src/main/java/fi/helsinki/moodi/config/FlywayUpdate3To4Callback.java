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

package fi.helsinki.moodi.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.stereotype.Component;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Component
@Order(HIGHEST_PRECEDENCE)
public class FlywayUpdate3To4Callback implements Callback {

    private final Logger logger = LoggerFactory.getLogger(FlywayUpdate3To4Callback.class);

    private final Flyway flyway;

    public FlywayUpdate3To4Callback(@Lazy Flyway flyway) {
        this.flyway = flyway;
    }

    private boolean checkColumnExists(Configuration flywayConfiguration) throws MetaDataAccessException {
        return (boolean) JdbcUtils.extractDatabaseMetaData(flywayConfiguration.getDataSource(),
            callback -> callback
                .getColumns(null, null, flywayConfiguration.getTable(), "version_rank")
                .next());
    }

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_VALIDATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    @Override
    public void handle(Event event, Context context) {
        boolean versionRankColumnExists = false;
        try {
            versionRankColumnExists = checkColumnExists(context.getConfiguration());
        } catch (MetaDataAccessException e) {
            logger.error("Cannot obtain flyway metadata");
            return;
        }
        if (versionRankColumnExists) {
            logger.info("Upgrading metadata table the Flyway 4.0 format ...");
            Resource resource = new ClassPathResource("db/migration/psql/flyway_upgradeMetaDataTable_V3_to_V4.sql",
                Thread.currentThread().getContextClassLoader());
            ScriptUtils.executeSqlScript(context.getConnection(), resource);
            logger.info("Flyway metadata table updated successfully.");
            // recalculate checksums
            flyway.repair();
        }
    }
}

