package fi.helsinki.moodi.service.importing;

import java.io.Serializable;

public final class ImportCourseResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public final long moodleCourseId;

    public ImportCourseResponse(long moodleCourseId) {
        this.moodleCourseId = moodleCourseId;
    }
}
