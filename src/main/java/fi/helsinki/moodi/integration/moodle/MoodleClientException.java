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

package fi.helsinki.moodi.integration.moodle;

import fi.helsinki.moodi.exception.MoodiException;

public final class MoodleClientException extends MoodiException {

    private final String moodleException;
    private final String errorCode;

    public MoodleClientException(String message, Throwable cause) {
        super(message, cause);
        this.moodleException = null;
        this.errorCode = null;
    }

    public MoodleClientException(String message, String moodleException, String errorCode) {
        super(message);
        this.moodleException = moodleException;
        this.errorCode = errorCode;
    }

    public String getMoodleException() {
        return moodleException;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
