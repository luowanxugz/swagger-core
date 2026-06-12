package io.swagger.v3.jaxrs2;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class JakartaAnnotationCompatibilityTest {

    @Test
    public void testJakartaHttpMethodAndPathAnnotationsAreResolved() {
        Reader reader = new Reader(new OpenAPI());
        OpenAPI openAPI = reader.read(JakartaResource.class);

        PathItem pathItem = openAPI.getPaths().get("/jakarta/items");
        assertNotNull(pathItem);
        assertNotNull(pathItem.getGet());
        assertEquals(pathItem.getGet().getOperationId(), "listItems");
    }

    @jakarta.ws.rs.Path("/jakarta")
    public static class JakartaResource {
        @jakarta.ws.rs.GET
        @jakarta.ws.rs.Path("/items")
        public String listItems() {
            return "ok";
        }
    }
}
