package fi.helsinki.moodi.web;

import fi.helsinki.moodi.service.Result;
import fi.helsinki.moodi.service.dto.CourseDto;
import fi.helsinki.moodi.service.importing.ImportCourseRequest;
import fi.helsinki.moodi.service.importing.ImportCourseResponse;
import fi.helsinki.moodi.service.importing.ImportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
public class CourseController {

    private final ImportingService importingService;

    @Autowired
    public CourseController(ImportingService importingService) {
        this.importingService = importingService;
    }

    @RequestMapping(value = "/api/v1/courses", method = RequestMethod.POST)
    public ResponseEntity<Result<ImportCourseResponse, String>> importCourse(
        @Valid @RequestBody final ImportCourseRequest request) {

        return response(importingService.importCourse(request));
    }

    @RequestMapping(value = "/api/v1/courses/{realisationId}", method = RequestMethod.GET)
    public ResponseEntity<CourseDto> getCourse(
        @PathVariable("realisationId") Long realisationId) {

        return response(importingService.getCourse(realisationId));
    }

    private <D, E> ResponseEntity<Result<D, E>> response(Result<D, E> result) {
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private <T> ResponseEntity<T> response(T entity) {
        return new ResponseEntity<>(entity, HttpStatus.OK);
    }
}
