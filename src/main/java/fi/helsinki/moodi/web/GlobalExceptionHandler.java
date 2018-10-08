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

import fi.helsinki.moodi.exception.*;
import fi.helsinki.moodi.service.Result;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static org.slf4j.LoggerFactory.getLogger;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static String STATUS_ERROR = "error";
    private static String STATUS_CONNECTION_ERROR = "connection-error";
    private static String STATUS_NOT_FOUND = "not-found";

    private static final Logger logger = getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = IntegrationConnectionException.class)
    public ResponseEntity<Result<?, String>> handleIntegrationConnectionException(IntegrationConnectionException e) throws Exception {
        logger.error("Caught an exception", e);
        return new ResponseEntity(Result.error("Connection error", STATUS_CONNECTION_ERROR, e.getMessage()), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(value = NotFoundException.class)
    public ResponseEntity<Result<?, String>> handleNotFoundException(NotFoundException e) throws Exception {
        logger.error("Caught an exception", e);
        return new ResponseEntity(Result.error(e.getMessage(), STATUS_NOT_FOUND, e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = CourseNotFoundException.class)
    public ResponseEntity<Result<?, String>> handleCourseNotFoundException(CourseNotFoundException e) throws Exception {
        logger.error(e.getMessage());
        return new ResponseEntity(Result.error(e.getMessage(), STATUS_NOT_FOUND, e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = MoodiException.class)
    public ResponseEntity<Result<?, String>> handleMoodiException(MoodiException e) throws Exception {
        logger.error("Caught an exception", e);
        return new ResponseEntity(Result.error("Error", STATUS_ERROR, e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = UnauthorizedException.class)
    public ResponseEntity<Result<?, String>> handleUnauthorizedException(Exception e) throws Exception {
        logger.error("Caught an exception", e);
        return new ResponseEntity(Result.error("Error", STATUS_ERROR, e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Result<?, String>> handleException(Exception e) throws Exception {
        logger.error("Caught an exception", e);
        return new ResponseEntity(Result.error("Error", STATUS_ERROR, "Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
