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
import java.util.List;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockSisuServer {
    private final MockServerClient client;

    public MockSisuServer(MockServerClient client) {
        this.client = client;
    }

    public void expectCourseUnitRealisationRequest(String curId, String responseFile) {
        Arguments arguments = new Arguments("course_unit_realisation", new Argument<>("id", curId));
        expectRequest(SisuCourseUnitRealisation.class, responseFile, arguments);
    }

    public void expectPersonsRequest(List<String> personIds, String responseFile) {
        Arguments arguments = new Arguments("private_persons", new Argument<>("ids", personIds));
        expectRequest(SisuPerson.SisuPersonWrapper.class, responseFile, arguments);
    }

    private <T> void expectRequest(Class<T> requestClass, String responseFile, Arguments... arguments) {
        client
            .when(
                request()
                    .withMethod("POST")
                    .withPath("/graphql")
                    .withHeader("X-Api-Key", "test-apikey")
                    .withBody(requestBodyMatcher(requestClass, arguments)))
            .respond(
                response()
                    .withStatusCode(200)
                    .withBody(Fixtures.asString(responseFile)));
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
