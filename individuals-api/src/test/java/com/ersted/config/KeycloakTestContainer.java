package com.ersted.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.util.Map;

public interface KeycloakTestContainer {

    GenericContainer<?> KEYCLOAK = createKeycloak();

    static GenericContainer<?> createKeycloak() {
        GenericContainer<?> container = new GenericContainer<>("quay.io/keycloak/keycloak:26.2")
                .withCommand("start-dev", "--import-realm")
                .withExposedPorts(8080, 9000)
                .withEnv(Map.of(
                                "KC_BOOTSTRAP_ADMIN_USERNAME", "admin",
                                "KC_BOOTSTRAP_ADMIN_PASSWORD", "admin",
                                "KC_HEALTH_ENABLED", "true"
                        )
                )
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("keycloak/realm-export.json"),
                        "/opt/keycloak/data/import/realm.json"
                )
                .waitingFor(Wait.forHttp("/health/ready")
                        .forPort(9000)
                        .forStatusCode(200)
                );


        container.start();
        return container;
    }

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("keycloak.url", () -> "http://" + KEYCLOAK.getHost() + ":" + KEYCLOAK.getMappedPort(8080));
        registry.add("keycloak.realm", () -> "payment-system");
        registry.add("keycloak.client-id", () -> "individuals-api");
        registry.add("keycloak.client-secret", () -> "test-secret-for-integration-tests");
    }

}
