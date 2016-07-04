package fi.helsinki.moodi.service.synchronize.process;

import fi.helsinki.moodi.service.course.CourseService;
import fi.helsinki.moodi.service.synchronize.SynchronizationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Processor implementation that removes course from the Moodi database.
 */
@Component
public class RemovingProcessor extends AbstractProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemovingProcessor.class);

    private final CourseService courseService;

    @Autowired
    public RemovingProcessor(CourseService courseService) {
        super(Action.REMOVE);
        this.courseService = courseService;
    }

    @Override
    protected SynchronizationItem doProcess(final SynchronizationItem item) {
        courseService.delete(item.getCourse().id);
        return item.completeProcessingPhase(ProcessingStatus.SUCCESS, "Removed", true);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
