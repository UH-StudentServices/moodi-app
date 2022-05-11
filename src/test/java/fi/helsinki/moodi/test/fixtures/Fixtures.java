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

package fi.helsinki.moodi.test.fixtures;

import com.google.common.io.Files;
import fi.helsinki.moodi.test.templates.Templates;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class Fixtures {

    private static final String PREFIX = "src/test/resources/fixtures/";

    public static String asString(final String path) {
        return asString(path, new HashMap<>());
    }

    public static String asString(final String path, final Map<String, ?> variables) {
        return asString(PREFIX, path, variables);
    }

    public static String asString(final String prefix, final String path, final Map<String, ?> variables) {
        try {
            final String content = Files.asCharSource(new File(prefix + path), StandardCharsets.UTF_8).read();
            return Templates.render(content, variables);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static FixtureBuilder builder(final String path) {
        return new FixtureBuilder(path);
    }

    public static final class FixtureBuilder {

        private final String path;
        private final Map<String, Object> variables;

        private FixtureBuilder(String path) {
            this.path = path;
            this.variables = new HashMap<>();
        }

        public FixtureBuilder withVariable(final String name, final Object value) {
            variables.put(name, value);
            return this;
        }

        public String build() {
            return "";
        }

    }
}
