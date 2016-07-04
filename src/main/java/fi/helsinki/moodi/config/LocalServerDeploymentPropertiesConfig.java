package fi.helsinki.moodi.config;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = { "file:${user.home}/moodi/moodi.yml" })
@Conditional(LocalServerDeploymentCondition.class)
public class LocalServerDeploymentPropertiesConfig {
}
