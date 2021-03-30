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

package fi.helsinki.moodi.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.helsinki.moodi.integration.sisu.SisuCourseUnitRealisation;
import fi.helsinki.moodi.integration.sisu.SisuPerson;
import fi.helsinki.moodi.test.fixtures.Fixtures;
import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.internal.DefaultObjectMapperFactory;
import org.mockserver.client.MockServerClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockSisuGraphQLServer {
    private final MockServerClient client;
    private static final String API_KEY_HEADER_NAME = "X-Api-Key";
    private static final String API_KEY = "test-apikey";

    public MockSisuGraphQLServer(MockServerClient client) {
        this.client = client;
    }

    public void reset() {
        client.reset();
    }

    public void expectCourseUnitRealisationsRequest(List<String> curIds, String responseFile, Map<String, ?> variables) {
        Arguments arguments = new Arguments("course_unit_realisations", new Argument<>("ids", curIds));
        expectGraphqlRequest(responseFile, variables, SisuCourseUnitRealisation.SisuCURWrapper.class, arguments);
    }

    public void expectCourseUnitRealisationsRequest(List<String> curIds, String responseFile) {
        expectCourseUnitRealisationsRequest(curIds, responseFile, new HashMap<>());
    }

    public void expectPersonsRequest(List<String> personIds, String responseFile, Map<String, ?> variables) {
        Arguments arguments = new Arguments("private_persons", new Argument<>("ids", personIds));
        expectGraphqlRequest(responseFile, variables, SisuPerson.SisuPersonWrapper.class, arguments);
    }

    public void expectPersonsRequest(List<String> personIds, String responseFile) {
        expectPersonsRequest(personIds, responseFile, new HashMap<>());
    }

    private <T> void expectGraphqlRequest(String responseFile, final Map<String, ?> responseVariables,
                                          Class<T> requestClass, Arguments... requestArguments) {
        client
            .when(
                request()
                    .withMethod("POST")
                    .withPath("/graphql")
                    .withHeader(API_KEY_HEADER_NAME, API_KEY)
                    .withBody(requestBodyMatcher(requestClass, requestArguments)))
            .respond(
                response()
                    .withStatusCode(200)
                    .withBody(Fixtures.asString(responseFile, responseVariables)));
    }

    private <T> String requestBodyMatcher(Class<T> requestClass, Arguments... arguments) {
        GraphQLRequestEntity requestEntity;
        try {
            requestEntity = GraphQLRequestEntity.Builder()
                .url("http://not-used")
                .request(requestClass)
                .arguments(arguments)
                .scalars(LocalDate.class, LocalDateTime.class)
                .build();

            SisuServerRequest sisuServerRequest = new SisuServerRequest(requestEntity.getRequest(), requestEntity.getVariables());

            DefaultObjectMapperFactory defaultObjectMapperFactory = new DefaultObjectMapperFactory();
            ObjectMapper mapper = defaultObjectMapperFactory.newSerializerMapper();

            return mapper.writeValueAsString(sisuServerRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}