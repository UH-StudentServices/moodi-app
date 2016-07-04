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
