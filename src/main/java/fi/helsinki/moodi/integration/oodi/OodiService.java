package fi.helsinki.moodi.integration.oodi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OodiService {

    private final OodiClient oodiClient;

    @Autowired
    public OodiService(OodiClient oodiClient) {
        this.oodiClient = oodiClient;
    }

    public Optional<OodiCourseUnitRealisation> getOodiCourseUnitRealisation(final long realisationId) {
        return oodiClient.getCourseUnitRealisation(realisationId);
    }
}
