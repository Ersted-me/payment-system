package com.ersted.individualsapi.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CreateKeycloakUserRequest {
    private String username;
    private String email;
    private Boolean emailVerified;
    private Boolean enabled;
    private List<String> requiredActions;
    private List<Credential> credentials;
    private Map<String, List<String>> attributes;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class Credential {
        private String type;
        private String value;
        private Boolean temporary;
    }

}
