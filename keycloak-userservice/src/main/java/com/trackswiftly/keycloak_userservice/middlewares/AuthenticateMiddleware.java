package com.trackswiftly.keycloak_userservice.middlewares;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trackswiftly.keycloak_userservice.dtos.TrackSwiftlyRoles;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;

public class AuthenticateMiddleware {


    private static final Logger logger = LoggerFactory.getLogger(AuthenticateMiddleware.class);


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




    public static void checkRoleHierarchy(
            KeycloakSession session, 
            UserModel requestingUser, 
            UserModel targetUser, 
            GroupModel targetGroup) {
            
        if (session == null || requestingUser == null || targetUser == null || targetGroup == null) {
            logger.error("Security violation: Null parameters detected in checkRoleHierarchy");
            throw new BadRequestException("Incorrectly sent data");
        }

        RealmModel realm = session.getContext().getRealm();
        
        if (requestingUser.getId().equals(targetUser.getId())) {
            logger.warn("Security alert: User {} attempted to modify their own permissions", requestingUser.getId());
            throw new ForbiddenException("Self-modification of permissions is not allowed");
        }

        RoleModel adminRole = realm.getRole(TrackSwiftlyRoles.ADMIN.name());
        RoleModel managerRole = realm.getRole(TrackSwiftlyRoles.MANAGER.name());
        
        AuthenticationUtils.validateRolesExist(adminRole, managerRole);

        boolean isAdmin = requestingUser.hasRole(adminRole);
        
        if (!isAdmin) {
            boolean isManager = requestingUser.hasRole(managerRole);
            if (!isManager) {
                logger.warn("Unauthorized access attempt by user: {}", requestingUser.getId());
                throw new ForbiddenException("Insufficient permissions to modify users");
            }

            AuthenticationUtils.validateManagerPermissions(targetUser, adminRole, managerRole, targetGroup);
            
            // checkOrganizationAccess(provider, requestingUser, targetUser);
            
            AuthenticationUtils.validateGroupPermissions(requestingUser, targetGroup);
        }

        logger.info("User {} authorized to modify user {} in group {}", 
        requestingUser.getId(), targetUser.getId(), targetGroup.getName());
    }


    public static void preventUserFromUpdatingThemselves(
        UserModel requestingUser , UserModel targetUser
    ) {
        if (requestingUser.getId().equals(targetUser.getId())) {
            logger.warn("Security alert: User {} attempted to modify their own permissions", requestingUser.getId());
            throw new ForbiddenException("Self-modification of permissions is not allowed");
        }
    }

    /*****
     * 
     * 
     * Utils Functions
     */


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



