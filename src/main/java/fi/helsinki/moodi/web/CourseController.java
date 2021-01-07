/*
 * This file is part of Moodi application.
 *
 * Moodi application is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Moodi application is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Moodi application.  If not, see <http://www.gnu.org/licenses/>.
 */

package fi.helsinki.moodi.web;

import fi.helsinki.moodi.MoodiHealthIndicator;
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
    private final MoodiHealthIndicator moodiHealthIndicator;

    @Autowired
    public CourseController(ImportingService importingService, MoodiHealthIndicator moodiHealthIndicator) {
        this.importingService = importingService;
        this.moodiHealthIndicator = moodiHealthIndicator;
    }

    @RequestMapping(value = "/api/v1/courses", method = RequestMethod.POST)
    public ResponseEntity<Result<ImportCourseResponse, String>> importCourse(
        @Valid @RequestBody final ImportCourseRequest request) {

        try {
            Result<ImportCourseResponse, String> ret = importingService.importCourse(request);
            moodiHealthIndicator.clearError();
            return response(ret);
        } catch (Exception e) {
            moodiHealthIndicator.reportError(e.toString(), e);
            throw e;
        }
    }

    @RequestMapping(value = "/api/v1/courses/{realisationId}", method = RequestMethod.GET)
    public ResponseEntity<CourseDto> getCourse(
        @PathVariable("realisationId") String realisationId) {

        return response(importingService.getCourse(realisationId));
    }

    private <D, E> ResponseEntity<Result<D, E>> response(Result<D, E> result) {
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private <T> ResponseEntity<T> response(T entity) {
        return new ResponseEntity<>(entity, HttpStatus.OK);
    }
}
