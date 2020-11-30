package fi.helsinki.moodi.integration.sisu;

import java.util.List;

public interface SisuClient {

    SisuCourseUnitRealisation getCourseUnitRealisationData(String id);

    List<SisuPerson> getPersonData(List<String> ids);
}
