package fi.helsinki.moodi.config;

import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = { "file:/opt/moodi/config/moodi.yml" })
@Conditional(RemoteServerDeploymentCondition.class)
public class RemoteServerDeploymentPropertiesConfig {
}
