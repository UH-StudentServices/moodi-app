package fi.helsinki.moodi.service.util;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

@Service
@ConfigurationProperties(prefix="mapper.oodi")
public class MapperService {

    private static final String ROLE_STUDENT = "student";
    private static final String ROLE_TEACHER = "teacher";
    private static final String ROLE_MOODI = "moodi";

    private static final Logger LOGGER = getLogger(MapperService.class);

    private final Environment environment;

    private Map<String, String> organization = new HashMap<>();
    private String defaultCategory;

    private List<OrganisationMatcher> organisationMatchers;

    @Autowired
    public MapperService(final Environment environment) {
        this.environment = environment;
    }

    public void setOrganization(Map<String, String> organization) {
        this.organization = organization;
    }

    public void setDefaultCategory(String defaultCategory) {
        this.defaultCategory = defaultCategory;
    }

    public Map<String, String> getOrganization() {
        return organization;
    }

    @PostConstruct
    public void createOodiOrganisationPatterns() {
        organisationMatchers = organization.keySet()
                .stream()
                .sorted(new OrganisationMatcherPatternComparator())
                .map(s -> new OrganisationMatcher(s, organization.get(s)))
                .collect(Collectors.toList());
    }

    public String getMoodleCategory(final String oodiOrganization) {
        final String moodleCategory = organisationMatchers.stream()
                .filter(m -> m.test(oodiOrganization))
                .findFirst()
                .map(m -> m.moodleCategory)
                .orElseGet(() -> {
                        LOGGER.warn("Could not map Oodi organisation '" + oodiOrganization + "' to any Moodle category. Using default category " + defaultCategory);
                        return defaultCategory;
                    }
                );

        LOGGER.debug("Mapped oodi organisation '{}' to moodle category '{}'", oodiOrganization, moodleCategory);

        return moodleCategory;
    }

    public long getStudentRoleId() {
        return getMoodleRole(ROLE_STUDENT);
    }

    public long getTeacherRoleId() {
        return getMoodleRole(ROLE_TEACHER);
    }

    public long getMoodiRoleId() {
        return getMoodleRole(ROLE_MOODI);
    }

    public long getMoodleRole(final String role) {
        return environment.getRequiredProperty("mapper.moodle.role." + role, Long.class);
    }

    public static final class OrganisationMatcherPatternComparator implements Comparator<String> {

        @Override
        public int compare(final String a, final String b) {
            if (isWildcardPattern(a) && !isWildcardPattern(b)) {
                return 1;
            } else if (!isWildcardPattern(a) && isWildcardPattern(b)) {
                return -1;
            } else if (a.length() == b.length()) {
                return a.compareTo(b);
            } else {
                return new Integer(b.length()).compareTo(a.length());
            }
        }

        private boolean isWildcardPattern(String pattern) {
            return pattern.endsWith("*");
        }
    }

    private static class OrganisationMatcher implements Predicate<String> {

        private final Pattern pattern;
        private final String moodleCategory;

        private OrganisationMatcher(String patternString, String moodleCategory) {
            this.pattern = Pattern.compile(patternString.replace("*", ".*"));
            this.moodleCategory = moodleCategory;
        }

        @Override
        public boolean test(final String s) {
            return pattern.matcher(s).find();
        }
    }
}
