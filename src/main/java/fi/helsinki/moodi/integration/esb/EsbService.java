package fi.helsinki.moodi.integration.esb;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class EsbService {

    private static final Logger LOGGER = getLogger(EsbService.class);

    private final EsbClient esbClient;

    @Autowired
    public EsbService(EsbClient esbClient) {
        this.esbClient = esbClient;
    }

    public Optional<String> getStudentUsername(final String studentNumber) {
        return Optional
            .ofNullable(esbClient.getStudentUsername(studentNumber))
            .map(this::appendDomain);
    }

    public Optional<String> getTeacherUsername(final String teacherId) {
        final String normalizedTeacherId = "9" + teacherId;
        return Optional
            .ofNullable(esbClient.getTeacherUsername(normalizedTeacherId))
            .map(this::appendDomain);
    }

    private String appendDomain(final String username) {
        return username + "@helsinki.fi";
    }
}
