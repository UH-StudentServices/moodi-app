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
import graphql.ExecutionInput;
import graphql.ParseAndValidate;
import graphql.ParseAndValidateResult;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.validation.ValidationError;
import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.internal.DefaultObjectMapperFactory;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Delay;
import org.mockserver.model.HttpRequest;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockSisuGraphQLServer {
    private final MockServerClient client;
    private static final String API_KEY_HEADER_NAME = "X-Api-Key";
    private static final String API_KEY = "test-apikey";

    private final GraphQLSchema graphQLSchema;
    private List<HttpRequest> expectedRequests = new ArrayList<>();

    public MockSisuGraphQLServer(MockServerClient client) {
        this.client = client;

        File sisuSchema = new File("src/test/resources/graphql/sisu.graphql");
        TypeDefinitionRegistry typeDef = new SchemaParser().parse(sisuSchema);
        graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeDef, RuntimeWiring.newRuntimeWiring().build());
    }

    public void reset() {
        client.reset();
        expectedRequests.clear();
    }

    public void expectCourseUnitRealisationsRequest(List<String> curIds, String responseFile, Map<String, ?> variables) {
        Arguments arguments = new Arguments("course_unit_realisations", new Argument<>("ids", curIds));
        String responseString = Fixtures.asString(responseFile, variables);
        expectGraphqlRequest(responseString, SisuCourseUnitRealisation.SisuCURWrapper.class, arguments);
    }

    public void expectCourseUnitRealisationsRequest(List<String> curIds, String responseFile) {
        expectCourseUnitRealisationsRequest(curIds, responseFile, new HashMap<>());
    }

    public void expectCourseUnitRealisationsRequestFromString(List<String> curIds, String responseString) {
        Arguments arguments = new Arguments("course_unit_realisations", new Argument<>("ids", curIds));
        expectGraphqlRequestWithDelay(responseString, SisuCourseUnitRealisation.SisuCURWrapper.class, arguments);
    }

    public void expectPersonsRequest(List<String> personIds, String responseFile, Map<String, ?> variables) {
        Arguments arguments = new Arguments("private_persons", new Argument<>("ids", personIds));
        String responseString = Fixtures.asString(responseFile, variables);
        expectGraphqlRequest(responseString, SisuPerson.SisuPersonWrapper.class, arguments);
    }

    public void expectPersonsRequest(List<String> personIds, String responseFile) {
        expectPersonsRequest(personIds, responseFile, new HashMap<>());
    }

    public void verify() {
        if (!expectedRequests.isEmpty()) {
            client.verify(expectedRequests.toArray(new HttpRequest[0]));
        }
    }

    private <T> void expectGraphqlRequest(String responseString, Class<T> requestClass, Arguments... requestArguments) {
        HttpRequest request = request()
            .withMethod("POST")
            .withPath("/graphql")
            .withHeader(API_KEY_HEADER_NAME, API_KEY)
            .withBody(requestBodyMatcher(requestClass, requestArguments));

        client
            .when(request)
            .respond(
                response()
                    .withStatusCode(200)
                    .withBody(responseString));
        expectedRequests.add(request);
    }

    private <T> void expectGraphqlRequestWithDelay(String responseString, Class<T> requestClass, Arguments... requestArguments) {
        HttpRequest request = request()
            .withMethod("POST")
            .withPath("/graphql")
            .withHeader(API_KEY_HEADER_NAME, API_KEY)
            .withBody(requestBodyMatcher(requestClass, requestArguments));

        client
            .when(request)
            .respond(
                response()
                    .withStatusCode(200)
                    .withBody(responseString)
                    .withDelay(Delay.milliseconds(ThreadLocalRandom.current().nextInt(1001))));

        expectedRequests.add(request);
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

            String query = requestEntity.getRequest();
            assertQueryIsValid(query);

            SisuServerRequest sisuServerRequest = new SisuServerRequest(query, requestEntity.getVariables());

            DefaultObjectMapperFactory defaultObjectMapperFactory = new DefaultObjectMapperFactory();
            ObjectMapper mapper = defaultObjectMapperFactory.newSerializerMapper();

            return mapper.writeValueAsString(sisuServerRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assertQueryIsValid(String query) {
        ParseAndValidateResult result = ParseAndValidate.parseAndValidate(graphQLSchema, new ExecutionInput.Builder().query(query).build());
        List<ValidationError> errors = result.getValidationErrors();

        if (!errors.isEmpty()) {
            String errorsParsed = errors.stream().map(e -> e.getErrorType() + ", " + e.getMessage()).collect(Collectors.joining("; "));
            fail("Failed query validation. Details: " + errorsParsed);
        }
    }
}
