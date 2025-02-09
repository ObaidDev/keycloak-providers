package com.trackswiftly.keycloak_userservice.middlewares;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;

import com.trackswiftly.keycloak_userservice.dtos.TrackSwiftlyRoles;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;

public class AuthenticateMiddleware {


    private AuthenticateMiddleware () {}

    public static AuthResult checkAuthentication(KeycloakSession session) {
        AuthResult auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        
        if (auth == null) {
			throw new NotAuthorizedException("Bearer");
		}

        return auth ;
    }


    public static void checkRealm(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        String realmName = realm.getName();

        if (!realmName.toLowerCase().matches(".*?(track|swiftly).*")) {
            throw new ForbiddenException("This Endpoint is only available in realms containing 'track' or 'swiftly' in their name");
        }
    }


    public static void checkRole(
        AuthResult auth ,
        KeycloakSession session ,
        List<TrackSwiftlyRoles> roleNames
    ) {

        UserModel authenticatedUser = auth.getUser();

        if (!userHasAnyRole(session, authenticatedUser, roleNames)) {
            throw new ForbiddenException("You do not have the required roles: " + 
                roleNames.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(", ")));
        }
    }



    public static void checkOrganizationAccess(
        OrganizationProvider provider, 
        UserModel adminUser, 
        UserModel targetUser
    ) {
        Stream<OrganizationModel> adminOrganizations = provider.getByMember(adminUser);
        Stream<OrganizationModel> targetUserOrganizations = provider.getByMember(targetUser);

        boolean sharedOrganization = adminOrganizations
            .anyMatch(adminOrg -> 
                targetUserOrganizations.anyMatch(targetOrg -> 
                    targetOrg.getId().equals(adminOrg.getId())
                )
            );

        if (!sharedOrganization) {
            throw new ForbiddenException("Users must be in the same organization");
        }
    }



    public static UserModel checkOrganizationAccess(

        KeycloakSession session, 
        OrganizationProvider provider, 
        UserModel adminUser, 
        String targetUserId

    ) {
        RealmModel realm = session.getContext().getRealm();
        UserModel targetUser = session.users().getUserById(realm, targetUserId);


        if (targetUser == null) {
            throw new NotFoundException("User not found");
        }
        
        Stream<OrganizationModel> adminOrganizations = provider.getByMember(adminUser);
        Stream<OrganizationModel> targetUserOrganizations = provider.getByMember(targetUser);

        boolean sharedOrganization = adminOrganizations
            .anyMatch(adminOrg -> 
                targetUserOrganizations.anyMatch(targetOrg -> 
                    targetOrg.getId().equals(adminOrg.getId())
                )
            );

        if (!sharedOrganization) {
            // Users must be in the same organization
            throw new ForbiddenException("access denied or unable to process the item");
        }

        return targetUser;
    }





    /***
     * 
     * @ authorization to ensure managers can't modify users at the same or higher level.
     * 
     */





    public static void checkRoleHierarchy(KeycloakSession session , UserModel requestingUser, UserModel targetUser) {

        RealmModel realm = session.getContext().getRealm();
        
        // Get the role models
        RoleModel adminRole = realm.getRole(TrackSwiftlyRoles.ADMIN.name());
        RoleModel managerRole = realm.getRole(TrackSwiftlyRoles.MANAGER.name());
        
        if (adminRole == null || managerRole == null) {
            throw new IllegalStateException("Required roles are not configured in the realm");
        }

        // Check if requesting user has admin role
        boolean isAdmin = requestingUser.hasRole(adminRole);
        
        // If not admin, check if they have manager role
        if (!isAdmin) {
            boolean isManager = requestingUser.hasRole(managerRole);
            if (!isManager) {
                throw new ForbiddenException("Insufficient permissions to modify users");
            }

            // Manager-specific restrictions
            // Check if target user has admin role (directly or through groups)
            if (targetUser.hasRole(adminRole)) {
                throw new ForbiddenException("Managers cannot modify admin accounts");
            }

            // Check if target user has manager role (directly or through groups)
            if (targetUser.hasRole(managerRole)) {
                throw new ForbiddenException("Managers cannot modify other manager accounts");
            }
        }
    }




    /**
     * Checks if the user has any of the specified roles.
     *
     * @param session the Keycloak session
     * @param user the user to check
     * @param roleNames the list of role names to check against
     * @return true if the user has any of the roles, false otherwise
     */
    private static boolean userHasAnyRole(KeycloakSession session, UserModel user, List<TrackSwiftlyRoles> roleNames) {
        RealmModel realm = session.getContext().getRealm();
        
        return roleNames.stream()
            .map(roleName -> realm.getRole(roleName.name()))
            .filter(Objects::nonNull)
            .anyMatch(user::hasRole);
    }
}   