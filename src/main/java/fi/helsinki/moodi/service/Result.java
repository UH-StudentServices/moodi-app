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
