package com.ersted.walletservice.spec.integration.controller;

import com.ersted.walletservice.spec.integration.LifecycleSpecification;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTest extends LifecycleSpecification {


    @Test
    void protectedGetWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/v1/wallets/user/{userUuid}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedPostWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(post("/v1/wallets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedGetWithValidTokenShouldNotReturn401() throws Exception {
        mockMvc.perform(get("/v1/wallets/user/{userUuid}", UUID.randomUUID())
                        .with(jwt()))
                .andExpect(status().is(not(equalTo(401))));
    }


    @Test
    void swaggerUiShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().is(not(equalTo(401))));
    }

    @Test
    void apiDocsShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().is(not(equalTo(401))));
    }

    @Test
    void actuatorHealthShouldBeAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is(not(equalTo(401))));
    }

}
