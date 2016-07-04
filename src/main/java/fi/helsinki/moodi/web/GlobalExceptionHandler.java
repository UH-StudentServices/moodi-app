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
