package fi.helsinki.moodi.scheduled;

import fi.helsinki.moodi.service.course.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CleanImportStatuses {

    private final CourseService courseService;

    @Autowired
    public CleanImportStatuses(CourseService courseService) {
        this.courseService = courseService;
    }

    // Run once per 2 hours
    @Scheduled(initialDelay = 0, fixedDelay = 7200000)
    public void execute() {
        courseService.cleanImportStatuses();
    }
}
