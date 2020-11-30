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

import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLResponseEntity;
import io.aexp.nodes.graphql.GraphQLTemplate;
import io.aexp.nodes.graphql.exceptions.GraphQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SisuGraphQLClient implements SisuClient {

    private static final Logger log = LoggerFactory.getLogger(SisuGraphQLClient.class);

    private static final String API_KEY_HEADER_NAME = "X-Api-Key";

    private final Map<String, String> headers = new HashMap<>();
    private final String endPointURL;

    public SisuGraphQLClient(String sisuBaseUrl, String apiKey) {
        this.endPointURL = sisuBaseUrl + "/graphql";
        this.headers.put(API_KEY_HEADER_NAME, apiKey);
    }

    @Override
    public SisuCourseUnitRealisation getCourseUnitRealisationData(String id) {
        Arguments idArgument = new Arguments("course_unit_realisation", new Argument<>("id", id));
        return queryWithArguments(SisuCourseUnitRealisation.class, idArgument);
    }

    @Override
    public List<SisuPerson> getPersonData(List<String> ids) {
        Arguments idArgument = new Arguments("private_persons", new Argument<>("ids", ids));
        return queryWithArguments(SisuPerson.SisuPersonWrapper.class, idArgument).private_persons;
    }

    private <T> T queryWithArguments(Class<T> type, Arguments... arguments) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        GraphQLTemplate graphQLTemplate = new GraphQLTemplate();

        try {
            GraphQLRequestEntity requestEntity = GraphQLRequestEntity.Builder()
                .url(endPointURL)
                .headers(headers)
                .request(type)
                .arguments(arguments)
                .scalars(LocalDate.class, LocalDateTime.class)
                .build();

            GraphQLResponseEntity<T> responseEntity = graphQLTemplate.query(requestEntity, type);

            return responseEntity.getResponse();
        } catch (Exception e) {
            String desc = e instanceof GraphQLException ? ((GraphQLException) e).getDescription() : "";
            throw new RuntimeException("GraphQL query failed with exception. " + desc, e);
        } finally {
            stopWatch.stop();
            log.info("GrapQL query {} took {} seconds", Arrays.asList(arguments), stopWatch.getTotalTimeSeconds());
        }
    }
}
