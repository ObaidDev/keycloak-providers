package com.trackswiftly.keycloak_userservice.middlewares;

import java.util.List;
import java.util.Objects;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trackswiftly.keycloak_userservice.dtos.TrackSwiftlyRoles;

import jakarta.ws.rs.ForbiddenException;

public class AuthenticationUtils {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationUtils.class);


    AuthenticationUtils() {
        
    }



    public static boolean userHasAnyRole(KeycloakSession session, UserModel user, List<TrackSwiftlyRoles> roleNames) {
        RealmModel realm = session.getContext().getRealm();
        
        return roleNames.stream()
            .map(roleName -> realm.getRole(roleName.name()))
            .filter(Objects::nonNull)
            .anyMatch(user::hasRole);
    }



    public static void validateRolesExist(RoleModel... roles) {
        for (RoleModel role : roles) {
            if (role == null) {
                logger.error("Security configuration error: Required role not found in realm");
                throw new IllegalStateException("Required roles are not configured in the realm");
            }
        }
    }

    public static void validateManagerPermissions(
            UserModel targetUser, 
            RoleModel adminRole, 
            RoleModel managerRole,
            GroupModel targetGroup) {
            
        // Check if target user has admin role
        if (targetUser.hasRole(adminRole)) {
            logger.warn("Manager attempted to modify admin account: {}", targetUser.getId());
            throw new ForbiddenException("Managers cannot modify admin accounts");
        }

        // Check if target user has manager role
        if (targetUser.hasRole(managerRole)) {
            logger.warn("Manager attempted to modify another manager account: {}", targetUser.getId());
            throw new ForbiddenException("Managers cannot modify other manager accounts");
        }

        // Validate sensitive group assignments
        if (isAdminGroup(targetGroup) || isManagerGroup(targetGroup)) {
            logger.warn("Manager attempted to assign user to privileged group: {}", targetGroup.getName());
            throw new ForbiddenException("Managers cannot assign users to admin or manager groups");
        }
    }


    public static void validateGroupPermissions(UserModel requestingUser, GroupModel targetGroup) {
        // Verify the requesting user has access to the target group
        boolean hasGroupAccess = requestingUser.getGroupsStream()
            .anyMatch(g -> g.getId().equals(targetGroup.getId()) || 
                         isParentGroup(g, targetGroup));

        if (!hasGroupAccess) {
            logger.warn("User {} attempted to assign to unauthorized group: {}", 
                requestingUser.getId(), targetGroup.getName());
            throw new ForbiddenException("No permission to assign users to this group");
        }
    }




    private static boolean isParentGroup(GroupModel potentialParent, GroupModel targetGroup) {
        return targetGroup.getParentId() != null && 
               targetGroup.getParentId().equals(potentialParent.getId());
    }

    private static boolean isAdminGroup(GroupModel group) {
        return group.getName().toLowerCase().contains("admin");
    }

    private static boolean isManagerGroup(GroupModel group) {
        return group.getName().toLowerCase().contains("manager");
    }
}
