package fi.helsinki.moodi.service.importing;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

public final class ImportCourseRequest implements Serializable {

    @NotNull
    public final Long realisationId;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ImportCourseRequest(Long realisationId) {
        this.realisationId = realisationId;
    }
}
