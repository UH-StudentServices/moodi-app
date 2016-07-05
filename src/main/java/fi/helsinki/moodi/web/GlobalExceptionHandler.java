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

import fi.helsinki.moodi.exception.IntegrationConnectionException;
import fi.helsinki.moodi.exception.MoodiException;
import fi.helsinki.moodi.exception.NotFoundException;
import fi.helsinki.moodi.exception.UnauthorizedException;
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

    private static final Logger LOGGER = getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = IntegrationConnectionException.class)
    public ResponseEntity<Result<?, String>> handleIntegrationConnectionException(IntegrationConnectionException e) throws Exception {
        LOGGER.error("Caught an exception", e);
        return new ResponseEntity(Result.error("Connection error", "connection-error", e.getMessage()), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(value = NotFoundException.class)
    public ResponseEntity<Result<?, String>> handleNotFoundException(NotFoundException e) throws Exception {
        LOGGER.error("Caught an exception", e);
        return new ResponseEntity(Result.error(e.getMessage(), "not-found", e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = MoodiException.class)
    public ResponseEntity<Result<?, String>> handleMoodiException(MoodiException e) throws Exception {
        LOGGER.error("Caught an exception", e);
        return new ResponseEntity(Result.error("Error", "error", e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = UnauthorizedException.class)
    public ResponseEntity<Result<?, String>> handleUnauthorizedException(Exception e) throws Exception {
        LOGGER.error("Caught an exception", e);
        return new ResponseEntity(Result.error("Error", "error", e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Result<?, String>> handleException(Exception e) throws Exception {
        LOGGER.error("Caught an exception", e);
        return new ResponseEntity(Result.error("Error", "error", "Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
