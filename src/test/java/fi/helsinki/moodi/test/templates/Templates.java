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

package fi.helsinki.moodi.test.templates;

import org.slf4j.Logger;
import org.trimou.engine.MustacheEngine;
import org.trimou.engine.MustacheEngineBuilder;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public final class Templates {

    private static final Logger LOGGER = getLogger(Templates.class);

    private static final MustacheEngine ENGINE =
            MustacheEngineBuilder.newBuilder().build();

    public static String render(final String templateContent, final Map<String, ?> variables) {
        LOGGER.debug("About to render template {}", templateContent.length() < 5000 ?
                templateContent :
                templateContent.length() + " characters");

        final String content = ENGINE.compileMustache(
                String.valueOf(templateContent.hashCode()),
                templateContent).render(variables);

        LOGGER.debug("Rendered template");

        return content;
    }
}
