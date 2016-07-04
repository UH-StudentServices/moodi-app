package fi.helsinki.moodi.service;

import java.util.Optional;

public class Result<D, E> {

    public final Optional<D> data;
    public final Optional<E> error;
    public final String status;
    public final String message;
    public final boolean success;

    private Result(Optional<D> data, Optional<E> error, String status, String message, boolean success) {
        this.data = data;
        this.error = error;
        this.status = status;
        this.message = message;
        this.success = success;
    }

    public static <D, E> Result<D, E> success(D data) {
        return new Result<>(Optional.of(data), Optional.<E>empty(), "success", "Success", true);
    }

    public static <D, E> Result<D, E> error(E error, String status, String message) {
        return new Result<>(Optional.empty(), Optional.of(error), status, message, false);
    }

}
