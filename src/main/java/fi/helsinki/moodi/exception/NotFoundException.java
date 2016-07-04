package fi.helsinki.moodi.exception;

import java.util.function.Supplier;

public class NotFoundException extends MoodiException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }

    public static Supplier<NotFoundException> notFoundException(final String message) {
        return () -> new NotFoundException(message);
    }
}

