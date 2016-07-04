package fi.helsinki.moodi.service.synchronize.enrich;

import fi.helsinki.moodi.exception.MoodiException;

public final class EnrichException extends MoodiException {

    public EnrichException(String message, Throwable cause) {
        super(message, cause);
    }
}
