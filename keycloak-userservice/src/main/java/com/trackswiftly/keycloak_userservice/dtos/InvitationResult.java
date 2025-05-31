package com.trackswiftly.keycloak_userservice.dtos;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class InvitationResult {
    private String email;
    private String userId;
    private boolean success;
    private String message;
    private String errorCode;


    public InvitationResult(String email, String userId, boolean success, String message) {
        this.email = email;
        this.userId = userId;
        this.success = success;
        this.message = message;
    }
}
