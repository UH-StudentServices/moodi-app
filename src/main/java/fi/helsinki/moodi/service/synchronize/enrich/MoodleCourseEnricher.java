package fi.helsinki.moodi.service.synchronize.enrich;

import com.google.common.collect.Lists;
import fi.helsinki.moodi.integration.moodle.MoodleFullCourse;
import fi.helsinki.moodi.integration.moodle.MoodleService;
import fi.helsinki.moodi.service.course.Course;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MoodleCourseEnricher extends AbstractEnricher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MoodleCourseEnricher.class);

    private final MoodleService moodleService;

    @Autowired
    public MoodleCourseEnricher(MoodleService moodleService) {
        super(10);
        this.moodleService = moodleService;
    }

    @Override
    protected SynchronizationItem doEnrich(final SynchronizationItem item) {
        final Course course = item.getCourse();
        final List<MoodleFullCourse> courses = moodleService.getCourses(Lists.newArrayList(course.moodleId));

        if (courses.isEmpty()) {
            return item.completeEnrichmentPhase(
                    EnrichmentStatus.MOODLE_COURSE_NOT_FOUND,
                    "Course not found from Moodle with id " + course.moodleId);
        } else {
            return item.setMoodleCourse(courses.stream().findFirst());
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}