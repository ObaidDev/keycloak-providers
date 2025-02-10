package com.trackswiftly.keycloak_userservice.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

import com.trackswiftly.keycloak_userservice.middlewares.AuthenticateMiddleware;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

public class UserManagementService {
    

    private static final  String USER_NOT_NULL_MESSAGE = "Target user must not be null" ;

    private final KeycloakSession session;
    private final RealmModel realm;

    public UserManagementService(KeycloakSession session) {
       this.session = session;
       this.realm = session.getContext().getRealm();
    }



    public Response toggleUserStatus(UserModel user, boolean enabled) {
       
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



    /***
     * 
     * user assignement .
     * @param user
     * @param group
     * @return
     */

    
    public Response assignUserToGroup(UserModel requestingUser, UserModel targetUser, GroupModel group) {
        
        Response inputValidation = validateInputs(requestingUser, targetUser, group);
        if (inputValidation != null) {
            return inputValidation;
        }


        Objects.requireNonNull(targetUser, USER_NOT_NULL_MESSAGE);
        Objects.requireNonNull(requestingUser, USER_NOT_NULL_MESSAGE);
        Objects.requireNonNull(group, "Group must not be null");

        try {
            
            targetUser.joinGroup(group);
            
            return Response.ok(Map.of(
                "userId", targetUser.getId(),
                "username", targetUser.getUsername(),
                "groupId", group.getId(),
                "groupName", group.getName(),
                "modifiedBy", requestingUser.getUsername()
            )).build();
        } catch (ForbiddenException e) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("error", e.getMessage()))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to assign user to group: " + e.getMessage())
                .build();
        }
    }



    public Response unassignUserFromGroup(UserModel requestingUser, UserModel targetUser, GroupModel group) {
        
        Response inputValidation = validateInputs(requestingUser, targetUser, group);
        if (inputValidation != null) {
            return inputValidation;
        }


        Objects.requireNonNull(targetUser, USER_NOT_NULL_MESSAGE);
        Objects.requireNonNull(requestingUser, USER_NOT_NULL_MESSAGE);
        Objects.requireNonNull(group, "Group must not be null");

        try {
            
            targetUser.leaveGroup(group);
            
            return Response.ok(Map.of(
                "userId", targetUser.getId(),
                "username", targetUser.getUsername(),
                "groupId", group.getId(),
                "groupName", group.getName(),
                "modifiedBy", requestingUser.getUsername()
            )).build();
        } catch (ForbiddenException e) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("error", e.getMessage()))
                .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to assign user to group: " + e.getMessage())
                .build();
        }
    }



    public Response userDetails(UserModel targetUser) {
        
        Map<String, Object> userDetails = new HashMap<>();

        userDetails.put("id", targetUser.getId());
        userDetails.put("username", targetUser.getUsername());
        userDetails.put("email", targetUser.getEmail());


        Set<String> roles = targetUser.getRoleMappingsStream()
                .map(RoleModel::getName)
                .collect(Collectors.toSet());
        userDetails.put("roles", roles);


        Set<String> groups = targetUser.getGroupsStream()
                .map(GroupModel::getName)
                .collect(Collectors.toSet());
        userDetails.put("groups", groups);

        return Response.ok(userDetails).build();
    }





    private Response validateInputs(UserModel requestingUser, UserModel targetUser, GroupModel group) {
        if (requestingUser == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("Requesting user not found")
                .build();
        }

        if (targetUser == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("Target user not found")
                .build();
        }

        if (group == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("Group not found")
                .build();
        }

        return null;
    }
}
