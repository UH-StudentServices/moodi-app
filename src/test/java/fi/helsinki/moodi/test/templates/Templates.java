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
        LOGGER.debug("About to render template {}", templateContent);

        final String content = ENGINE.compileMustache(
                String.valueOf(templateContent.hashCode()),
                templateContent).render(variables);

        LOGGER.debug("Rendered template: {}", content);

        return content;
    }
}