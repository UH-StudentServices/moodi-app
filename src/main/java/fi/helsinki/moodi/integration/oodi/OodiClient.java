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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fi.helsinki.moodi.integration.util.RestUtil.buildResourceUri;
import static fi.helsinki.moodi.util.DateFormat.OODI_UTC_DATE_FORMAT;

public class OodiClient {

    private final String baseUrl;
    private final RestOperations restOperations;

    public OodiClient(String baseUrl, RestOperations restOperations) {
        this.baseUrl = baseUrl;
        this.restOperations = restOperations;
    }

    public Optional<OodiCourseUnitRealisation> getCourseUnitRealisation(final long courseRealisationId) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("include_deleted", String.valueOf(true));
        params.add("include_approved_status", String.valueOf(true));

        return getOodiData(
            buildResourceUri(baseUrl, params, "courseunitrealisations", String.valueOf(courseRealisationId)),
            new ParameterizedTypeReference<OodiResponse<OodiCourseUnitRealisation>>() {});
    }

    public Optional<OodiCourseUsers> getCourseUsers(final long courseRealisationId) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("include_deleted", String.valueOf(true));
        params.add("include_approved_status", String.valueOf(true));

        return getOodiData(
            buildResourceUri(baseUrl, params,"courseunitrealisations", String.valueOf(courseRealisationId), "users"),
            new ParameterizedTypeReference<OodiResponse<OodiCourseUsers>>() {
            });
    }

    public List<OodiCourseChange> getCourseChanges(final LocalDateTime afterDate) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(OODI_UTC_DATE_FORMAT);
        final String formattedStartDate = formatter.format(afterDate);
        return getOodiData(
            buildResourceUri(baseUrl, "courseunitrealisations", "changes", "ids", formattedStartDate),
            new ParameterizedTypeReference<OodiResponse<List<OodiCourseChange>>>() {}
            ).orElse(new ArrayList<>());
    }

    private <T> Optional<T> getOodiData(
            final URI uri,
            final ParameterizedTypeReference<OodiResponse<T>> typeReference) {

        try {
            return Optional.ofNullable(
                    restOperations
                        .exchange(uri, HttpMethod.GET, null, typeReference).getBody())
                .map(body -> body.data);
        } catch (ResourceAccessException e) {
            throw new IntegrationConnectionException("Oodi connection failure", e);
        }
    }
}
