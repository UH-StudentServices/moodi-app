package fi.helsinki.moodi.integration.oodi;

import java.io.Serializable;
import java.util.List;

public class OodiCourseChangeRealisation implements Serializable {

    private static final long serialVersionUID = 1L;

    public Data data;

    public static class Data implements Serializable {

        private static final long serialVersionUID = 1L;

        public List<String> info;
        public List<String> opettajat;
        public List<String> alitoteutukset;
        public List<String> organisaatiot;
        public List<String> periodit;
    }
}
