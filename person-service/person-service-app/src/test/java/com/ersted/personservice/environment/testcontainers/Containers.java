package com.ersted.personservice.environment.testcontainers;

import org.testcontainers.containers.PostgreSQLContainer;

public class Containers {

    public static final PostgreSQLContainer<?> POSTGRES = PostgresTestContainer.POSTGRES;

    public static void run() {
        POSTGRES.start();
    }

}
