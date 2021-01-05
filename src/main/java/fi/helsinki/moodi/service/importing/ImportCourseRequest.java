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

package fi.helsinki.moodi.service.importing;

import javax.validation.constraints.NotNull;
import java.util.Objects;

public final class ImportCourseRequest {
    @NotNull
    public String realisationId;

    public ImportCourseRequest() {}

    public ImportCourseRequest(String i) {
        this.realisationId = i;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ImportCourseRequest that = (ImportCourseRequest) o;
        return Objects.equals(realisationId, that.realisationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(realisationId);
    }
}
