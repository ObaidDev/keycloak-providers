package com.trackswiftly.keycloak_userservice.services;

import java.util.Map;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import jakarta.ws.rs.core.Response;

public class UserManagementService {
    

    private final KeycloakSession session;
    private final RealmModel realm;

    public UserManagementService(KeycloakSession session) {
       this.session = session;
       this.realm = session.getContext().getRealm();
    }



    public Response toggleUserStatus(UserModel user, boolean enabled) {
    //    UserModel user = session.users().getUserById(realm, userId);
       
        if (user == null) {
           return Response.status(Response.Status.NOT_FOUND)
               .entity("User not found")
               .build();
        }

       user.setEnabled(enabled);

        return Response.ok(Map.of(
           "id", user.getId(),
           "username", user.getUsername(),
           "enabled", user.isEnabled()
        )).build();
    }
}
