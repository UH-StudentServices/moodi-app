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

package fi.helsinki.moodi.integration.sisu;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SisuOrganisation {
    public static final String ACTIVE = "ACTIVE";
    public String id;
    public LocalDateTime snapshotDateTime;
    public SisuLocalisedValue name;
    public String code;
    public String parentId;
    public String status;
    public String documentState;

    public SisuOrganisation() {}

    public SisuOrganisation(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static class SisuOrganisationExportBatch {
        public int greatestOrdinal;
        public boolean hasMore;
        public List<SisuOrganisation> entities = new ArrayList<>();
    }
}
