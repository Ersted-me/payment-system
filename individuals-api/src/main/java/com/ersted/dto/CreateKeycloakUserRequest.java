package com.ersted.dto;

import lombok.*;

import java.util.List;

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
