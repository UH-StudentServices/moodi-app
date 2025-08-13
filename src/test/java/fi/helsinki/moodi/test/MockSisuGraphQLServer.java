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
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
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

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.fail;

public class MockSisuGraphQLServer {
    private final WireMockServer wireMockServer;
    private static final String API_KEY_HEADER_NAME = "X-Api-Key";
    private static final String API_KEY = "test-apikey";

    private final GraphQLSchema graphQLSchema;
    private List<String> expectedRequests = new ArrayList<>();

    public MockSisuGraphQLServer(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;

        File sisuSchema = new File("src/test/resources/graphql/sisu.graphql");
        TypeDefinitionRegistry typeDef = new SchemaParser().parse(sisuSchema);
        graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeDef, RuntimeWiring.newRuntimeWiring().build());
    }

    public void reset() {
        wireMockServer.resetAll();
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

    public void expectCourseUnitRealisationsRequestFromString(List<String> curIds, String responseString, boolean delayed) {
        Arguments arguments = new Arguments("course_unit_realisations", new Argument<>("ids", curIds));
        if (delayed) {
            expectGraphqlRequestWithDelay(responseString, SisuCourseUnitRealisation.SisuCURWrapper.class, arguments);
        } else {
            expectGraphqlRequest(responseString, SisuCourseUnitRealisation.SisuCURWrapper.class, arguments);
        }
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
            for (String requestBody : expectedRequests) {
                List<LoggedRequest> requests = wireMockServer.findAll(
                    postRequestedFor(urlEqualTo("/graphql"))
                        .withHeader(API_KEY_HEADER_NAME, equalTo(API_KEY))
                        .withRequestBody(equalToJson(requestBody))
                );
                if (requests.isEmpty()) {
                    fail("Expected request not found: " + requestBody);
                }
            }
        }
    }

    private <T> void expectGraphqlRequest(String responseString, Class<T> requestClass, Arguments... requestArguments) {
        String requestBody = requestBodyMatcher(requestClass, requestArguments);
        
        wireMockServer.stubFor(
            post(urlEqualTo("/graphql"))
                .withHeader(API_KEY_HEADER_NAME, equalTo(API_KEY))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(responseString)
                        .withHeader("Content-Type", "application/json")
                )
        );
        
        expectedRequests.add(requestBody);
    }

    private <T> void expectGraphqlRequestWithDelay(String responseString, Class<T> requestClass, Arguments... requestArguments) {
        String requestBody = requestBodyMatcher(requestClass, requestArguments);
        
        wireMockServer.stubFor(
            post(urlEqualTo("/graphql"))
                .withHeader(API_KEY_HEADER_NAME, equalTo(API_KEY))
                .withRequestBody(equalToJson(requestBody))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withBody(responseString)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(ThreadLocalRandom.current().nextInt(1001))
                )
        );

        expectedRequests.add(requestBody);
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