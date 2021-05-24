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

package fi.helsinki.moodi.graphql;

import fi.helsinki.moodi.integration.sisu.SisuCourseUnitRealisation;
import fi.helsinki.moodi.integration.sisu.SisuPerson;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import org.junit.Test;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

public class ValidateSisuGraphQLQueriesTest {

    private final GraphQLSchema schema;
    private final Parser graphqlParser;
    private final Validator validator;

    public ValidateSisuGraphQLQueriesTest() {
        File schemaFile = new File("src/test/resources/graphql/sisu.graphql");
        TypeDefinitionRegistry typeDef = new SchemaParser().parse(schemaFile);
        schema = new SchemaGenerator().makeExecutableSchema(typeDef, RuntimeWiring.newRuntimeWiring().build());
        graphqlParser = new Parser();
        validator = new Validator();
    }

    @Test
    public void thatCourseUnitRealisationsQueryIsValid() throws Exception {
        assertQueryIsValid(
            SisuCourseUnitRealisation.SisuCURWrapper.class,
            new Arguments("course_unit_realisations", new Argument<>("ids", Collections.singletonList("cur-id"))));
    }

    @Test
    public void thatPersonsQueryIsValid() throws Exception {
        assertQueryIsValid(
            SisuPerson.SisuPersonWrapper.class,
            new Arguments("private_persons", new Argument<>("ids", Collections.singletonList("person-id"))));
    }

    private <T> void assertQueryIsValid(Class<T> type, Arguments... arguments) throws Exception {
        Document query = getQuery(type, arguments);
        List<ValidationError> errors = validator.validateDocument(schema, query);
        if (!errors.isEmpty()) {
            String errorsString = errors.stream().map(e -> e.getErrorType() + ", " + e.getMessage()).collect(Collectors.joining("; "));
            fail("Failed query validation, details: " + errorsString);
        }
    }

    private <T> Document getQuery(Class<T> type, Arguments... arguments) throws Exception {
        String query = GraphQLRequestEntity.Builder()
            .url("https://example.org/not-used")
            .request(type)
            .arguments(arguments)
            .scalars(LocalDate.class, LocalDateTime.class)
            .build().getRequest();

        return graphqlParser.parseDocument(query);
    }
}
