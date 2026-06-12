package io.swagger.v3.jaxrs2.integration;

import com.my.project.jakartaresources.JakartaResourceInPackageC;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.testng.Assert.assertTrue;

public class JaxrsAnnotationScannerJakartaTest {

    @Test
    public void shouldScanJakartaAnnotatedClassesFromPackages() {
        JaxrsAnnotationScanner scanner = new JaxrsAnnotationScanner();
        SwaggerConfiguration openApiConfiguration = new SwaggerConfiguration();
        openApiConfiguration.setResourcePackages(Collections.singleton("com.my.project.jakartaresources"));
        scanner.application(null).openApiConfiguration(openApiConfiguration);

        assertTrue(scanner.classes().contains(JakartaResourceInPackageC.class));
    }
}
