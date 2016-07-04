package fi.helsinki.moodi.service.synchronize.process;

import fi.helsinki.moodi.exception.MoodiException;

public final class ProcessException extends MoodiException {

    public ProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
