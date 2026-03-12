package com.ersted.individualsapi.spec.integration;

import com.ersted.individualsapi.IndividualsApiApplication;
import com.ersted.individualsapi.client.KeycloakClient;
import com.ersted.individualsapi.enviroment.testcontainers.container.Containers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {IndividualsApiApplication.class}
)
@ActiveProfiles("test")
public abstract class LifecycleSpecification {

    @LocalServerPort
    private int port;

    protected WebTestClient webTestClient;

    @Autowired
    protected KeycloakClient keycloakClient;

    @BeforeAll
    void setUpWebTestClient() {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    static {
        Containers.run();
    }

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("keycloak.url", () -> "http://" + Containers.KEYCLOAK.getHost() + ":" + Containers.KEYCLOAK.getMappedPort(8080));
        registry.add("keycloak.realm", () -> "payment-system");
        registry.add("keycloak.client-id", () -> "individuals-api");
        registry.add("keycloak.client-secret", () -> "test-secret-for-integration-tests");
    }

}
