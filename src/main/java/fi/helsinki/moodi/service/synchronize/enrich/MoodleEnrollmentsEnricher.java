package fi.helsinki.moodi.service.synchronize.enrich;

import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.integration.moodle.MoodleUserEnrollments;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MoodleEnrollmentsEnricher extends AbstractEnricher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoodleEnrollmentsEnricher.class);

    private final MoodleService moodleService;

    @Autowired
    public MoodleEnrollmentsEnricher(MoodleService moodleService) {
        super(20);
        this.moodleService = moodleService;
    }

    @Override
    protected SynchronizationItem doEnrich(final SynchronizationItem item) {
        final Course course = item.getCourse();
        final List<MoodleUserEnrollments> moodleEnrollments = moodleService.getEnrolledUsers(course.moodleId);
        return item.setMoodleEnrollments(Optional.of(moodleEnrollments));
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}