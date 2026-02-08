package com.ersted.enviroment.testcontainers.container;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.util.Map;

public class KeycloakTestContainer {

    public static final GenericContainer<?> KeycloakTestContainer;

    static {
        KeycloakTestContainer = new GenericContainer<>("quay.io/keycloak/keycloak:26.2")
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


    }

}
