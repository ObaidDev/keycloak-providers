package com.trackswiftly.keycloak_userservice.dtos;



import org.keycloak.models.UserModel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@AllArgsConstructor
@Builder
@Data
public class ProcessedInvitation {

    private final InvitationResult result;
    private final InvitationRequest request;
    private final UserModel user;
    private final String invitationLink;


    public ProcessedInvitation(InvitationResult result, InvitationRequest request) {
        this.result = result;
        this.request = request;
        this.user = null;
        this.invitationLink = null;
    }

}
