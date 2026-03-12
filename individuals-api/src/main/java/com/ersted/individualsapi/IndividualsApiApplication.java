package com.ersted.individualsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.ersted.individualsapi.config")
public class IndividualsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndividualsApiApplication.class, args);
    }

}
