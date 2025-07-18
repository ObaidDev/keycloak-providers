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
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import org.keycloak.services.resources.KeycloakOpenAPI;

import com.trackswiftly.keycloak_userservice.dtos.InvitationRequest;
import com.trackswiftly.keycloak_userservice.dtos.TrackSwiftlyRoles;
import com.trackswiftly.keycloak_userservice.middlewares.AuthenticateMiddleware;
import com.trackswiftly.keycloak_userservice.services.OrganizationInvitationService;
import com.trackswiftly.keycloak_userservice.services.UserManagementService;
import com.trackswiftly.keycloak_userservice.utils.CorsUtils;
import com.trackswiftly.keycloak_userservice.utils.EmailValidator;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;



@Slf4j
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


    @OPTIONS
    @Path("{any:.*}")
    public Response preflight() {
        log.debug("Preflight request received for CORS");
        return Cors.builder()
                .preflight()
                .auth()
                .allowAllOrigins()
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .exposedHeaders("Location")
                .add(Response.ok());
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
    public Response inviteUser
        (
            @FormParam("email") String email,
            @FormParam("firstName") String firstName,
            @FormParam("lastName") String lastName ,
            @Context HttpHeaders headers
        ) {
        
        
        AuthenticateMiddleware.checkRealm(session);
        AuthResult authResult = AuthenticateMiddleware.checkAuthentication(session) ;
        AuthenticateMiddleware.checkRole(authResult, session, List.of(TrackSwiftlyRoles.ADMIN , TrackSwiftlyRoles.MANAGER)) ;
        
        /***
         * 
         *  get the first org of the crreunt user , we will allow to the user to be part of just one org .
         */

        Stream<OrganizationModel> organizations = provider.getByMember(authResult.getUser());

        Optional<OrganizationModel> firstOrganization = organizations.findFirst();
        
        Response response ;
        if (firstOrganization.isPresent()) {
            OrganizationModel organization = firstOrganization.get();

            response =  new OrganizationInvitationService(session, organization).inviteUser(email, firstName, lastName);

        } else {

            return Response.status(Response.Status.NOT_FOUND)
                           .entity(NO_ORGANIZATION_FOUND_FOR_USR)
                           .build();
        }

        return CorsUtils.addCorsHeaders(response, headers);
    }

    /**
     * Bulk invite multiple users - simple version like the original single invite
     * @param userInvitations Simple list of user data (email required, firstName/lastName optional)
     * @return BulkInvitationResponse with results for each invitation
     */
    @Path("invite-users-bulk")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Invites multiple users at once",
            description = "Sends invitations or registration links to multiple users in a single request. " +
                         "Automatically handles existing vs new users like the single invite method. " +
                         "Uses a single SMTP connection for better performance.")
    public Response inviteUsersBulk(
            @Valid List<@Valid InvitationRequest> userInvitations,
            @Context HttpHeaders headers
        ) {
        
        AuthenticateMiddleware.checkRealm(session);
        AuthResult authResult = AuthenticateMiddleware.checkAuthentication(session);
        AuthenticateMiddleware.checkRole(authResult, session, List.of(TrackSwiftlyRoles.ADMIN, TrackSwiftlyRoles.MANAGER));
        


        Response response ;

        // Validate the request before processing
        EmailValidator.ValidationResult validationResult = EmailValidator.validateInvitationRequests(userInvitations);
        if (!validationResult.isValid()) {
            
            response =  Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of(
                            "error", "VALIDATION_ERROR",
                            "message", "Invalid request data",
                            "details", validationResult.getErrors()
                        ))
                        .build();

            return CorsUtils.addCorsHeaders(response, headers);
        }

        // Get the first org of the current user
        Stream<OrganizationModel> organizations = provider.getByMember(authResult.getUser());
        Optional<OrganizationModel> firstOrganization = organizations.findFirst();
        

        
        if (firstOrganization.isPresent()) {
            OrganizationModel organization = firstOrganization.get();
            
            response = new OrganizationInvitationService(session, organization).inviteMultipleUsers(userInvitations);
        } else {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(NO_ORGANIZATION_FOUND_FOR_USR)
                           .build();
        }


        return CorsUtils.addCorsHeaders(response, headers);
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
    public Response getRealmGroups(
        @Context HttpHeaders headers
    ) {

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
        

        return CorsUtils.addCorsHeaders(Response.ok(groups).build(), headers);
    }


    @POST
    @Path("groups/{group}/users/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response assignUserToGroup(
        @PathParam("userId") String userId, 
        @PathParam("group") String groupName,
        @Context HttpHeaders headers
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

        Response response = new UserManagementService(session).assignUserToGroup(requestingUser, targetUser, group) ;

        return CorsUtils.addCorsHeaders(response, headers) ;        
    }



    @DELETE
    @Path("groups/{group}/users/{userId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unAssignUserFromGroup(
        @PathParam("userId") String userId, 
        @PathParam("group") String groupName,
        @Context HttpHeaders headers
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

        Response response = new UserManagementService(session).unassignUserFromGroup(requestingUser, targetUser, group) ; 


        return CorsUtils.addCorsHeaders(response, headers) ;
    }



    @Path("users")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsers(
        @Context HttpHeaders headers
    ) {
        
        
        AuthenticateMiddleware.checkRealm(session);
        AuthResult authResult = AuthenticateMiddleware.checkAuthentication(session) ;
        AuthenticateMiddleware.checkRole(authResult, session, List.of(TrackSwiftlyRoles.ADMIN , TrackSwiftlyRoles.MANAGER)) ;
        
        /***
         * 
         *  get the first org of the crreunt user , we will allow to the user to be part of just one org .
         */

        Stream<OrganizationModel> organizations = provider.getByMember(authResult.getUser());

        Optional<OrganizationModel> firstOrganization = organizations.findFirst();
        

        Response response;
        if (firstOrganization.isPresent()) {
            OrganizationModel organization = firstOrganization.get();

            response =  new OrganizationInvitationService(session, organization).getOrgMembers(provider , 0 , 20);

        } else {

            return Response.status(Response.Status.NOT_FOUND)
                           .entity(NO_ORGANIZATION_FOUND_FOR_USR)
                           .build();
        }


        return CorsUtils.addCorsHeaders(response, headers) ;
    }




    @Path("users/{userId}/status")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response toggleUser(
        @PathParam("userId") String userId, 
        @QueryParam("enabled") boolean enabled ,
        @Context HttpHeaders headers
    ) {
        AuthenticateMiddleware.checkRealm(session);

        AuthResult authResult = AuthenticateMiddleware.checkAuthentication(session);

        AuthenticateMiddleware.checkRole(authResult, session, 
            List.of(TrackSwiftlyRoles.ADMIN));
        
        
        UserModel requestingUser = authResult.getUser() ;

        UserModel targetUser = AuthenticateMiddleware.checkOrganizationAccess(
            session, 
            provider, 
            requestingUser , 
            userId
        );

        AuthenticateMiddleware.preventUserFromUpdatingThemselves(requestingUser, targetUser);
        
        Response response =  new UserManagementService(session).toggleUserStatus(targetUser, enabled);
        
        return CorsUtils.addCorsHeaders(response, headers) ;
    }



    @Path("users/{userId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(
        @PathParam("userId") String userId ,
        @Context HttpHeaders headers
    ) {
        AuthenticateMiddleware.checkRealm(session);
        
        AuthResult authResult = AuthenticateMiddleware.checkAuthentication(session);

        AuthenticateMiddleware.checkRole(authResult, session, 
            List.of(TrackSwiftlyRoles.ADMIN , TrackSwiftlyRoles.MANAGER));
        
        
        UserModel requestingUser = authResult.getUser() ;

        UserModel targetUser = AuthenticateMiddleware.checkOrganizationAccess(
            session, 
            provider, 
            requestingUser , 
            userId
        );        

        Response response =  new UserManagementService(session).userDetails(targetUser) ;

        return CorsUtils.addCorsHeaders(response, headers) ;
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
