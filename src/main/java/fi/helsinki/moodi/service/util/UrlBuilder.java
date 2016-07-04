package fi.helsinki.moodi.service.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class UrlBuilder {

    private final Environment environment;

    @Autowired
    public UrlBuilder(Environment environment) {
        this.environment = environment;
    }

    public String getMoodleCourseUrl(Long moodleId) {
        return environment.getRequiredProperty("integration.moodle.baseUrl") + "/course/view.php?id=" + moodleId;
    }
}
