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

import fi.helsinki.moodi.exception.IntegrationConnectionException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fi.helsinki.moodi.util.DateFormat.OODI_UTC_DATE_FORMAT;

public class OodiClient {

    private final String baseUrl;
    private final RestOperations restOperations;

    public OodiClient(String baseUrl, RestOperations restOperations) {
        this.baseUrl = baseUrl;
        this.restOperations = restOperations;
    }

    public Optional<OodiCourseUnitRealisation> getCourseUnitRealisation(final long courseRealisationId) {
        return getOodiData(
                "{baseUrl}/courseunitrealisations/{realisationId}?include_deleted=true&include_approved_status=true",
                new ParameterizedTypeReference<OodiResponse<OodiCourseUnitRealisation>>() {
                },
                baseUrl,
                courseRealisationId);
    }

    public Optional<OodiCourseUsers> getCourseUsers(final long courseRealisationId) {
        return getOodiData(
            "{baseUrl}/courseunitrealisations/{realisationId}/users?include_deleted=true&include_approved_status=true",
            new ParameterizedTypeReference<OodiResponse<OodiCourseUsers>>() {
            },
            baseUrl,
            courseRealisationId);
    }

    public List<OodiCourseChange> getCourseChanges(final LocalDateTime afterDate) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(OODI_UTC_DATE_FORMAT);
        final String formattedStartDate = formatter.format(afterDate);
        return getOodiData(
                "{baseUrl}/courseunitrealisations/changes/ids/{startDate}",
                new ParameterizedTypeReference<OodiResponse<List<OodiCourseChange>>>() {},
                baseUrl,
                formattedStartDate).orElse(new ArrayList<>());
    }

    private <T> Optional<T> getOodiData(
            final String url,
            final ParameterizedTypeReference<OodiResponse<T>> typeReference,
            final Object... uriVariables) {

        try {
            return Optional.ofNullable(
                    restOperations
                        .exchange(url, HttpMethod.GET, null, typeReference, uriVariables).getBody())
                .map(body -> body.data);
        } catch (ResourceAccessException e) {
            throw new IntegrationConnectionException("Oodi connection failure", e);
        }
    }
}
