package com.trackswiftly.keycloak_userservice;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.admin.resource.OrganizationInvitationResource;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import com.trackswiftly.keycloak_userservice.services.OrganizationInvitationService;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


public class TrackSwiftlyResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationProvider provider;
    private final OrganizationModel organization;


    public TrackSwiftlyResource(
		KeycloakSession session, OrganizationModel organization
	) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.provider = session.getProvider(OrganizationProvider.class);
        this.organization = organization ;
    }


    @GET
	@Path("hello")
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
    public Response helloAnonymous() {
		return Response.ok(Map.of("hello", session.getContext().getRealm().getName())).build();
	}




	@Path("org/invite-user")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.ORGANIZATIONS)
    @Operation(summary = "Invites an existing user or sends a registration link to a new user, based on the provided e-mail address.",
            description = "If the user with the given e-mail address exists, it sends an invitation link, otherwise it sends a registration link.")
    public Response inviteUser(@FormParam("email") String email,
                               @FormParam("firstName") String firstName,
                               @FormParam("lastName") String lastName) {
        return new OrganizationInvitationService(session, organization).inviteUser(email, firstName, lastName);
    }
    
}
