package fi.helsinki.moodi.exception;

public abstract class MoodiException extends RuntimeException {

    protected MoodiException(String message) {
        super(message);
    }

    protected MoodiException(String message, Throwable cause) {
        super(message, cause);
    }
}
