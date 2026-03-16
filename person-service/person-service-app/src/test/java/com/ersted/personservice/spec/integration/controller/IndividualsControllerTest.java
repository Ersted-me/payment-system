package com.ersted.personservice.spec.integration.controller;

import com.ersted.personservice.spec.integration.LifecycleSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class IndividualsControllerTest extends LifecycleSpecification {

    @Test
    void shouldCreateIndividualSuccessfully() throws Exception {
        mockMvc.perform(post("/v1/individuals")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateRequest("test@test.com", "RUS")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.passport_number").value("AB123456"))
                .andExpect(jsonPath("$.phone_number").value("+79001234567"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldCreateIndividualFailedWhenCountryNotFound() throws Exception {
        mockMvc.perform(post("/v1/individuals")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateRequest("test@test.com", "XXX")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void shouldGetIndividualSuccessfully() throws Exception {
        String location = mockMvc.perform(post("/v1/individuals")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateRequest("get@test.com", "RUS")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String userUuid = extractUuidFromLocation(location);

        mockMvc.perform(get("/v1/individuals/{userUuid}", userUuid)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userUuid))
                .andExpect(jsonPath("$.passport_number").value("AB123456"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldGetIndividualNotFound() throws Exception {
        UUID randomUuid = UUID.randomUUID();

        mockMvc.perform(get("/v1/individuals/{userUuid}", randomUuid)
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void shouldUpdateIndividualSuccessfully() throws Exception {
        String location = mockMvc.perform(post("/v1/individuals")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateRequest("update@test.com", "RUS")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String userUuid = extractUuidFromLocation(location);

        mockMvc.perform(patch("/v1/individuals/{userUuid}", userUuid)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passport_number": "ZZ999999",
                                  "phone_number": "+79009998877"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userUuid))
                .andExpect(jsonPath("$.passport_number").value("ZZ999999"))
                .andExpect(jsonPath("$.phone_number").value("+79009998877"));
    }

    @Test
    void shouldUpdateIndividualNotFound() throws Exception {
        UUID randomUuid = UUID.randomUUID();

        mockMvc.perform(patch("/v1/individuals/{userUuid}", randomUuid)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "passport_number": "ZZ999999"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void shouldActivateIndividualSuccessfully() throws Exception {
        String location = mockMvc.perform(post("/v1/individuals")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateRequest("activate@test.com", "RUS")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String userUuid = extractUuidFromLocation(location);

        mockMvc.perform(post("/v1/individuals/{userUuid}/active", userUuid)
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldActivateIndividualNotFound() throws Exception {
        UUID randomUuid = UUID.randomUUID();

        mockMvc.perform(post("/v1/individuals/{userUuid}/active", randomUuid)
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void shouldArchiveIndividualSuccessfully() throws Exception {
        String location = mockMvc.perform(post("/v1/individuals")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateRequest("archive@test.com", "RUS")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String userUuid = extractUuidFromLocation(location);

        mockMvc.perform(post("/v1/individuals/{userUuid}/archive", userUuid)
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldArchiveIndividualNotFound() throws Exception {
        UUID randomUuid = UUID.randomUUID();

        mockMvc.perform(post("/v1/individuals/{userUuid}/archive", randomUuid)
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void shouldPurgeIndividualSuccessfully() throws Exception {
        String location = mockMvc.perform(post("/v1/individuals")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateRequest("purge@test.com", "RUS")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");

        String userUuid = extractUuidFromLocation(location);

        mockMvc.perform(post("/v1/individuals/{userUuid}/purge", userUuid)
                        .with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnUnauthorizedWithoutJwt() throws Exception {
        mockMvc.perform(get("/v1/individuals/{userUuid}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private String buildCreateRequest(String email, String alpha3) {
        return """
                {
                  "passport_number": "AB123456",
                  "phone_number": "+79001234567",
                  "email": "%s",
                  "first_name": "John",
                  "last_name": "Doe",
                  "secret_key": "secret123",
                  "address": {
                    "address": "ул. Ленина, 1",
                    "zip_code": "101000",
                    "city": "Москва",
                    "state": "Московская область",
                    "country": {
                      "name": "Russia",
                      "alpha2": "RU",
                      "alpha3": "%s"
                    }
                  }
                }
                """.formatted(email, alpha3);
    }

    private String extractUuidFromLocation(String location) {
        if (location == null) {
            throw new IllegalStateException("Location header is missing");
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

}
