package fi.helsinki.moodi.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class DevMode implements Condition {

    @Override
    public boolean matches(
            final ConditionContext conditionContext,
            final AnnotatedTypeMetadata annotatedTypeMetadata) {

        return conditionContext.getEnvironment().getProperty("dev.mode.enabled", Boolean.class);
    }
}
