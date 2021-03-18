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

import fi.helsinki.moodi.exception.IntegrationConnectionException;
import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLResponseEntity;
import io.aexp.nodes.graphql.GraphQLTemplate;
import io.aexp.nodes.graphql.exceptions.GraphQLException;
import io.aexp.nodes.graphql.internal.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StopWatch;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static fi.helsinki.moodi.Constants.ACTIVE;

public class SisuClient {

    private static final Logger log = LoggerFactory.getLogger(SisuClient.class);

    private static final String API_KEY_HEADER_NAME = "X-Api-Key";
    private final String sisuBaseUrl;
    private final String apiKey;
    @Value("${SisuGraphQLClient.batchsize:100}")
    private int batchSize;
    private int connectTimeout;
    private int socketTimeout;
    private final RestOperations restOperations;

    public SisuClient(String sisuBaseUrl, String apiKey, int connectTimeout, int socketTimeout, RestOperations restOperations) {
        this.sisuBaseUrl = sisuBaseUrl;
        this.apiKey = apiKey;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.restOperations = restOperations;
    }

    // If even one of the requested IDs is not found, the whole query will return empty.
    // Should not happen, as Sisu courses should not refer to non-existing persons.
    public List<SisuPerson> getPersons(List<String> ids) {
        List<SisuPerson> ret = new ArrayList<>();
        for (List batchIds : splitToBatches(ids)) {
            Arguments idArgument = new Arguments("private_persons", new Argument<>("ids", batchIds));
            SisuPerson.SisuPersonWrapper result = queryWithArguments(SisuPerson.SisuPersonWrapper.class, idArgument);
            ret.addAll(result.private_persons);
        }
        return ret;
    }

    // If even one of the requested IDs is not found, the whole query will return empty.
    // Should not happen, since courses being synced existed during import.
    // But adding a broken ID into the DB manually would cause all IDs in the batch to go missing.
    public List<SisuCourseUnitRealisation> getCourseUnitRealisations(final List<String> ids) {
        List<SisuCourseUnitRealisation> ret = new ArrayList<>();
        for (List batchIds : splitToBatches(ids)) {
            Arguments idArgument = new Arguments("course_unit_realisations", new Argument<>("ids", batchIds));
            SisuCourseUnitRealisation.SisuCURWrapper result = queryWithArguments(SisuCourseUnitRealisation.SisuCURWrapper.class, idArgument);
            if (result != null) {
                ret.addAll(result.course_unit_realisations);
            }
        }
        return ret;
    }

    @Cacheable(value = "sisu-client.organisations-by-id", unless = "#result == null or #result.size()==0")
    public Map<String, SisuOrganisation> getAllOrganisationsById() {
        Map<String, SisuOrganisation> ret = new HashMap<>();
        List<SisuOrganisation> snapshots = new ArrayList<>();
        int since = 0;
        while (true) {
            SisuOrganisation.SisuOrganisationExportBatch batch =
                    getRestData(sisuBaseUrl + "/kori/api/organisations/v2/export?limit=10000&since=" + since,
                    new ParameterizedTypeReference<SisuOrganisation.SisuOrganisationExportBatch>() {});
            snapshots.addAll(batch.entities);
            if (!batch.hasMore) {
                break;
            }
            since = batch.greatestOrdinal;
        }
        Map<String, List<SisuOrganisation>> activeSnapshotsByOrgId = snapshots.stream()
                .filter(o -> ACTIVE.equals(o.documentState) && ACTIVE.equals(o.status))
                .collect(Collectors.groupingBy(SisuOrganisation::getId));

        // Take the latest snapshot for each organisation unit.
        activeSnapshotsByOrgId.entrySet().stream()
                .forEach(e -> ret.put(e.getKey(), e.getValue().stream().max(Comparator.comparing(sh -> sh.snapshotDateTime)).get()));

        return ret;
    }

    private <T> T getRestData(
            final String url,
            final ParameterizedTypeReference<T> typeReference) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(API_KEY_HEADER_NAME, apiKey);

            return restOperations.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), typeReference).getBody();
        } catch (ResourceAccessException e) {
            throw new IntegrationConnectionException("Sisu connection failure", e);
        }
    }

    private List<List<String>> splitToBatches(final List<String> ids) {
        final List<List<String>> batches = new ArrayList<>();
        final AtomicInteger counter = new AtomicInteger();
        for (String id : ids) {
            if (counter.getAndIncrement() % batchSize == 0) {
                batches.add(new ArrayList<>());
            }
            batches.get(batches.size() - 1).add(id);
        }
        return batches;
    }

    private <T> T queryWithArguments(Class<T> type, Arguments... arguments) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        GraphQLTemplate graphQLTemplate = new GraphQLTemplate(connectTimeout, socketTimeout);
        GraphQLResponseEntity<T> responseEntity;

        try {
            GraphQLRequestEntity requestEntity = GraphQLRequestEntity.Builder()
                .url(sisuBaseUrl + "/graphql")
                .headers(Collections.singletonMap(API_KEY_HEADER_NAME, apiKey))
                .request(type)
                .arguments(arguments)
                .scalars(LocalDate.class, LocalDateTime.class)
                .build();

            responseEntity = graphQLTemplate.query(requestEntity, type);
        } catch (Exception e) {
            String desc = e instanceof GraphQLException ? ((GraphQLException) e).getDescription() : "";
            throw new RuntimeException("GraphQL query failed with exception. " + desc, e);
        } finally {
            stopWatch.stop();
            log.info("GrapQL query {} took {} seconds", Arrays.asList(arguments), stopWatch.getTotalTimeSeconds());
        }
        if (responseEntity.getErrors() != null && responseEntity.getErrors().length > 0) {
            if (!isOne404(responseEntity)) {
                throw new RuntimeException("GraphQL query returned one or more errors. " +
                    Arrays.stream(responseEntity.getErrors()).map(Error::toString).reduce("", (a, b) -> a + b + "\n")
                );
            }
        }
        return responseEntity.getResponse();
    }

    private boolean isOne404(GraphQLResponseEntity responseEntity) {
        return responseEntity.getErrors() != null &&
            responseEntity.getErrors().length == 1 &&
            "404: Not Found".equals(responseEntity.getErrors()[0].getMessage());
    }
}
