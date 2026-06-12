package io.swagger.v3.jaxrs2.annotations.operations;

import io.swagger.v3.jaxrs2.annotations.AbstractAnnotationTest;
import io.swagger.v3.oas.annotations.Operation;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class JakartaOperationMethodTest extends AbstractAnnotationTest {

    @Test
    public void testJakartaPathAndGetOperation() {
        String openApiYAML = readIntoYaml(JakartaSimpleGetOperation.class);

        assertTrue(openApiYAML.contains("/jakarta/pets:"));
        assertTrue(openApiYAML.contains("get:"));
        assertTrue(openApiYAML.contains("operationId: listPets"));
    }

    @jakarta.ws.rs.Path("/jakarta")
    static class JakartaSimpleGetOperation {
        @Operation(operationId = "listPets")
        @jakarta.ws.rs.GET
        @jakarta.ws.rs.Path("/pets")
        public void listPets() {
        }
    }
}
