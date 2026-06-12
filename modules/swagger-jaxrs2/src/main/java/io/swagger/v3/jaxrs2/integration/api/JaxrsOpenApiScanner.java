package io.swagger.v3.jaxrs2.integration.api;

import io.swagger.v3.oas.integration.api.OpenApiScanner;

public interface JaxrsOpenApiScanner extends OpenApiScanner {

    void setApplication(Object application);
}
