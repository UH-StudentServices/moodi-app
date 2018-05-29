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
