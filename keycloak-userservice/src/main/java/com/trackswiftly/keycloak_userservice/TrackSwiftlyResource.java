package com.trackswiftly.keycloak_userservice;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import org.keycloak.services.resources.KeycloakOpenAPI;

import com.trackswiftly.keycloak_userservice.dtos.TrackSwiftlyRoles;
import com.trackswiftly.keycloak_userservice.middlewares.AuthenticateMiddleware;
import com.trackswiftly.keycloak_userservice.services.OrganizationInvitationService;
import com.trackswiftly.keycloak_userservice.services.UserManagementService;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


public class TrackSwiftlyResource {


    private static final String NO_ORGANIZATION_FOUND_FOR_USR = "No organization found for the user." ;

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationProvider provider;


    public TrackSwiftlyResource(
		KeycloakSession session
	) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.provider = session.getProvider(OrganizationProvider.class);
    }


    @POST
	@Path("hello/{group}/users/{userId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(
		summary = "Public hello endpoint",
		description = "This endpoint returns hello and the name of the requested realm."
	)
    @APIResponse(
		responseCode = "200",
		description = "",
		content = {@Content(
			schema = @Schema(
				implementation = Response.class,
				type = SchemaType.OBJECT
			)
		)}
	)
    public Response helloAnonymous(
        @PathParam("userId") String userId,
        @PathParam("group") String groupName
    ) {

      

		return Response.ok(Map.of("name" , "test" , "user" , "test")).build();
	}


    /*******
     * 
     * @param email
     * @param firstName
     * @param lastName
     * @return
     */

	@Path("invite-user")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Invites an existing user or sends a registration link to a new user, based on the provided e-mail address.",
            description = "If the user with the given e-mail address exists, it sends an invitation link, otherwise it sends a registration link.")
    public Response inviteUser(@FormParam("email") String email,
                               @FormParam("firstName") String firstName,
                               @FormParam("lastName") String lastName) {
        
        
        AuthenticateMiddleware.checkRealm(session);
        AuthResult authResult = AuthenticateMiddleware.checkAuthentication(session) ;
        AuthenticateMiddleware.checkRole(authResult, session, List.of(TrackSwiftlyRoles.ADMIN , TrackSwiftlyRoles.MANAGER)) ;
        
        /***
         * 
         *  get the first org of the crreunt user , we will allow to the user to be part of just one org .
         */

        Stream<OrganizationModel> organizations = provider.getByMember(authResult.getUser());

        Optional<OrganizationModel> firstOrganization = organizations.findFirst();
        
        if (firstOrganization.isPresent()) {
            OrganizationModel organization = firstOrganization.get();

            return new OrganizationInvitationService(session, organization).inviteUser(email, firstName, lastName);

        } else {

            return Response.status(Response.Status.NOT_FOUND)
                           .entity(NO_ORGANIZATION_FOUND_FOR_USR)
                           .build();
        }

    }


    /**
     * 
     * @return
     */

    @Path("groups")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Retrieve groups for the current realm")
    public Response getRealmGroups() {

        AuthenticateMiddleware.checkRealm(session);

        AuthResult authResult = AuthenticateMiddleware.checkAuthentication(session);
        
        AuthenticateMiddleware.checkRole(authResult, session, 
            List.of(TrackSwiftlyRoles.ADMIN, TrackSwiftlyRoles.MANAGER));
        
        Stream<GroupModel> groupsStream = session.getContext().getRealm().getGroupsStream();
        List<Map<String, String>> groups = groupsStream
            .map(group -> Map.of(
                "id", group.getId(), 
                "name", group.getName()
            ))
            .toList();
        
        return Response.ok(groups).build();
    }


    @POST
    @Path("groups/{group}/users/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignUserToGroup(
        @PathParam("userId") String userId, 
        @PathParam("group") String groupName
    ) {
        AuthResult authResult = AuthenticateMiddleware.checkAuthentication(session);
        
        AuthenticateMiddleware.checkRole(authResult, session, List.of(TrackSwiftlyRoles.ADMIN , TrackSwiftlyRoles.MANAGER)) ;
         
        /**
         * get the target user
         */
        UserModel targetUser = session.users().getUserById(session.getContext().getRealm(), userId);

        UserModel requestingUser = authResult.getUser() ;

        /**
         * 
         */

        AuthenticateMiddleware.checkOrganizationAccess(
            provider , 
            requestingUser , 
            targetUser
        );

        GroupModel group = session.groups().getGroupByName(realm, null, groupName.toUpperCase());


        AuthenticateMiddleware.checkRoleHierarchy(session, requestingUser, targetUser , group);

        return new UserManagementService(session).assignUserToGroup(requestingUser, targetUser, group) ;
        
    }



    @DELETE
    @Path("groups/{group}/users/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unAssignUserFromGroup(
        @PathParam("userId") String userId, 
        @PathParam("group") String groupName
    ) {
        AuthResult authResult = AuthenticateMiddleware.checkAuthentication(session);
        
        AuthenticateMiddleware.checkRole(authResult, session, List.of(TrackSwiftlyRoles.ADMIN , TrackSwiftlyRoles.MANAGER)) ;
        /**
         * get the target user
         */
        UserModel targetUser = session.users().getUserById(session.getContext().getRealm(), userId);

        UserModel requestingUser = authResult.getUser() ;


        /**
         * 
         */

        AuthenticateMiddleware.checkOrganizationAccess(
            provider, 
            requestingUser , 
            targetUser
        );


        GroupModel group = session.groups().getGroupByName(realm, null, groupName.toUpperCase());

        AuthenticateMiddleware.checkRoleHierarchy(session, requestingUser, targetUser , group);

        return new UserManagementService(session).unassignUserFromGroup(requestingUser, targetUser, group) ;  
    }



    @Path("users")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers() {
        
        
        AuthenticateMiddleware.checkRealm(session);
        AuthResult authResult = AuthenticateMiddleware.checkAuthentication(session) ;
        AuthenticateMiddleware.checkRole(authResult, session, List.of(TrackSwiftlyRoles.ADMIN , TrackSwiftlyRoles.MANAGER)) ;
        
        /***
         * 
         *  get the first org of the crreunt user , we will allow to the user to be part of just one org .
         */

        Stream<OrganizationModel> organizations = provider.getByMember(authResult.getUser());

        Optional<OrganizationModel> firstOrganization = organizations.findFirst();
        
        if (firstOrganization.isPresent()) {
            OrganizationModel organization = firstOrganization.get();

            return new OrganizationInvitationService(session, organization).getOrgMembers(provider , 0 , 20);

        } else {

            return Response.status(Response.Status.NOT_FOUND)
                           .entity(NO_ORGANIZATION_FOUND_FOR_USR)
                           .build();
        }

    }




    @Path("users/{userId}/status")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response toggleUser(
        @PathParam("userId") String userId, 
        @QueryParam("enabled") boolean enabled
    ) {
        AuthenticateMiddleware.checkRealm(session);
        AuthResult authResult = AuthenticateMiddleware.checkAuthentication(session);
        AuthenticateMiddleware.checkRole(authResult, session, 
            List.of(TrackSwiftlyRoles.ADMIN));
        
        
        UserModel requestinUser = authResult.getUser() ;
        UserModel targetUser = AuthenticateMiddleware.checkOrganizationAccess(
            session, 
            provider, 
            requestinUser , 
            userId
        );

        AuthenticateMiddleware.preventUserFromUpdatingThemselves(requestinUser, targetUser);
        

        return new UserManagementService(session).toggleUserStatus(targetUser, enabled);
    }


    @GET
	@Path("myorg")
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(
		summary = "Oragnization endpoint",
		description = "This endpoint returns Oranization of the current user ."
	)
    @APIResponse(
		responseCode = "200",
		description = "",
		content = {@Content(
			schema = @Schema(
				implementation = Response.class,
				type = SchemaType.OBJECT
			)
		)}
	)
    public Response myOrg() {

        AuthenticateMiddleware.checkRealm(session);
        AuthResult authResult = AuthenticateMiddleware.checkAuthentication(session) ;


        UserModel authenticatedUser = authResult.getUser();

        Stream<OrganizationModel> organizations = provider.getByMember(authenticatedUser);

        Optional<OrganizationModel> firstOrganization = organizations.findFirst();

        if (firstOrganization.isPresent()) {
            OrganizationModel organization = firstOrganization.get();

            return Response.ok(
                Map.of(
                    "name", organization.getName() ,
                    "id" , organization.getId()
                )
            ).build();
        } else {

            return Response.status(Response.Status.NOT_FOUND)
                           .entity(NO_ORGANIZATION_FOUND_FOR_USR)
                           .build();
        }

	}










    
}
