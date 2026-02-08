package com.ersted.enviroment.testcontainers.container;

import org.testcontainers.containers.GenericContainer;

public class Containers {

    public static final GenericContainer<?> KEYCLOAK = KeycloakTestContainer.KeycloakTestContainer;

    public static void run(){
        KEYCLOAK.start();
    }

}
