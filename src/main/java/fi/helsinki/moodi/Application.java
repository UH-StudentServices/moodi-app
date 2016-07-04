package fi.helsinki.moodi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class Application {

    public static void main(final String[] args) {
        final SpringApplication app = new SpringApplication(Application.class);
        addDefaultProfile(app, args);
        app.run(args);
    }

    private static void addDefaultProfile(final SpringApplication app, final String[] args) {
        final PropertySource source = new SimpleCommandLinePropertySource(args);
        if (!source.containsProperty("spring.profiles.active")) {
            app.setAdditionalProfiles("local");
        }
    }
}
