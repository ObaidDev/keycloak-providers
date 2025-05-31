

package com.trackswiftly.keycloak_userservice.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class InvitationRequest {
    

    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")   
    private String email;
    private String firstName;
    private String lastName;
    private String userId; // Optional, for existing users
}