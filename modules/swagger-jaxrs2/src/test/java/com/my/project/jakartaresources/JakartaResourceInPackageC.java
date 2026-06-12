package com.my.project.jakartaresources;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.ArrayList;

@jakarta.ws.rs.Path("/jakartaPackage")
public class JakartaResourceInPackageC {
    @Operation(operationId = "test.")
    @jakarta.ws.rs.GET
    public void getTest(@Parameter(name = "test") ArrayList<String> tenantId) {
        return;
    }
}
