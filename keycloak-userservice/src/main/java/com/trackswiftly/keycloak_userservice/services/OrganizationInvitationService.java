package com.trackswiftly.keycloak_userservice.services;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.storage.adapter.InMemoryUserAdapter;
import org.keycloak.utils.StringUtil;

import com.trackswiftly.keycloak_userservice.dtos.BulkInvitationResponse;
import com.trackswiftly.keycloak_userservice.dtos.InvitationRequest;
import com.trackswiftly.keycloak_userservice.dtos.InvitationResult;
import com.trackswiftly.keycloak_userservice.dtos.ProcessedInvitation;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.actiontoken.inviteorg.InviteOrgActionToken;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;

public class OrganizationInvitationService {


    private final KeycloakSession session;
    private final RealmModel realm;
    private final OrganizationModel organization;
    private final int tokenExpiration;
   
    

    public OrganizationInvitationService(KeycloakSession session, OrganizationModel organization) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.organization = organization;
        this.tokenExpiration = getTokenExpiration();
    }


    /***
     * Invit User , if exsit will be added , if not we will send registeration invinetation .
     * @param email
     * @param firstName
     * @param lastName
     * @return
     */

    public Response inviteUser(String email, String firstName, String lastName) {
        if (StringUtil.isBlank(email)) {
            throw ErrorResponse.error("To invite a member you need to provide an email", Status.BAD_REQUEST);
        }

        UserModel user = session.users().getUserByEmail(realm, email);

        if (user != null) {
            if (organization.isMember(user)) {
                throw ErrorResponse.error("User already a member of the organization", Status.CONFLICT);
            }

            return sendInvitation(user);
        }

        user = new InMemoryUserAdapter(session, realm, null);
        user.setEmail(email);

        if (firstName != null && lastName != null) {
            user.setFirstName(firstName);
            user.setLastName(lastName);
        }

        return sendInvitation(user);
    }



    public Response inviteExistingUser(String id) {
        if (StringUtil.isBlank(id)) {
            throw new BadRequestException("To invite a member you need to provide the user id");
        }

        UserModel user = session.users().getUserById(realm, id);

        if (user == null) {
            throw ErrorResponse.error("User does not exist", Status.BAD_REQUEST);
        }

        return sendInvitation(user);
    }

    private Response sendInvitation(UserModel user) {
        String link = user.getId() == null ? createRegistrationLink(user) : createInvitationLink(user);

        System.err.println(link + "✅");

        try {
            session.getProvider(EmailTemplateProvider.class)
                    .setRealm(realm)
                    .setUser(user)
                    .sendOrgInviteEmail(organization, link, TimeUnit.SECONDS.toMinutes(getActionTokenLifespan()));
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
            throw ErrorResponse.error("Failed to send invite email", Status.INTERNAL_SERVER_ERROR);
        }

        return Response.noContent().build();
    }

    
    private int getTokenExpiration() {
        return Time.currentTime() + getActionTokenLifespan();
    }

    private int getActionTokenLifespan() {
        return realm.getActionTokenGeneratedByAdminLifespan();
    }



    private String createInvitationLink(UserModel user) {
        return LoginActionsService.actionTokenProcessor(session.getContext().getUri())
                .queryParam("key", createToken(user))
                .build(realm.getName()).toString();
    }


    private String createRegistrationLink(UserModel user) {
        return OIDCLoginProtocolService.registrationsUrl(session.getContext().getUri().getBaseUriBuilder())
                .queryParam(OAuth2Constants.RESPONSE_TYPE, OIDCResponseType.CODE)
                .queryParam(Constants.CLIENT_ID, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)
                .queryParam(Constants.TOKEN, createToken(user))
                .buildFromMap(Map.of("realm", realm.getName(), "protocol", OIDCLoginProtocol.LOGIN_PROTOCOL)).toString();
    }




    private String createToken(UserModel user) {
        InviteOrgActionToken token = new InviteOrgActionToken(user.getId(), tokenExpiration, user.getEmail(), Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);

        token.setOrgId(organization.getId());

        if (organization.getRedirectUrl() == null || organization.getRedirectUrl().isBlank()) {
            token.setRedirectUri(Urls.accountBase(session.getContext().getUri().getBaseUri()).path("/").build(realm.getName()).toString());
        } else {
            token.setRedirectUri(organization.getRedirectUrl());
        }

        return token.serialize(session, realm, session.getContext().getUri());
    }




    public Response getOrgMembers(OrganizationProvider organizationProvider , int first , int max) {
        Stream<UserModel> users = organizationProvider.getMembersStream(
            organization, 
            Map.of(), 
            null, 
            first, 
            max
        );

        List<Map<String, Object>> userDetails = users
        .map(user -> {
            Map<String, Object> details = new HashMap<>();
            details.put("id", user.getId());
            details.put("username", user.getUsername());
            details.put("email", user.getEmail());
            details.put("firstName", user.getFirstName());
            details.put("lastName", user.getLastName());
            details.put("enabled", user.isEnabled());
            return details;
        })
        .toList();


        return Response.ok(userDetails).build() ;
    }




    /*
     * 
     * enhance fn of invitations :
     */


    public Response inviteMultipleUsers(List<InvitationRequest> invitationRequests) {
        if (invitationRequests == null || invitationRequests.isEmpty()) {
            throw new BadRequestException("Invitation requests list cannot be empty");
        }

        BulkInvitationResponse response = new BulkInvitationResponse();
        response.setTotalRequested(invitationRequests.size());

        // Process requests and prepare valid invitations
        List<ProcessedInvitation> validInvitations = new ArrayList<>();
        
        for (InvitationRequest request : invitationRequests) {
            InvitationResult result = processInvitationRequestWithoutSending(request);
            
            if (result.isSuccess()) {
                // Store the processed invitation for batch email sending
                validInvitations.add(new ProcessedInvitation(result, request));
            } else {
                // Add failed validation results immediately
                response.addResult(result);
            }
        }

        // Send all valid invitations in batch with single SMTP connection
        if (!validInvitations.isEmpty()) {
            List<InvitationResult> emailResults = sendBulkInvitations(validInvitations);
            for (InvitationResult result : emailResults) {
                response.addResult(result);
            }
        }

        return Response.ok(response).build();
    }
    




    /**
     * Process invitation request without sending email (validation only)
     */
    private InvitationResult processInvitationRequestWithoutSending(InvitationRequest request) {
        try {
            // Handle existing user invitation by ID
            if (!StringUtil.isBlank(request.getUserId())) {
                return validateExistingUserInvitation(request.getUserId());
            }

            // Handle invitation by email
            if (StringUtil.isBlank(request.getEmail())) {
                return new InvitationResult(request.getEmail(), null, false, 
                    "Email is required for invitation", "MISSING_EMAIL");
            }

            UserModel user = session.users().getUserByEmail(realm, request.getEmail());

            if (user != null) {
                // Existing user
                if (organization.isMember(user)) {
                    return new InvitationResult(request.getEmail(), user.getId(), false,
                        "User already a member of the organization", "ALREADY_MEMBER");
                }

                return new InvitationResult(request.getEmail(), user.getId(), true, "Ready for invitation");
            } else {
                // New user - will need registration invitation
                return new InvitationResult(request.getEmail(), null, true, "Ready for registration invitation");
            }

        } catch (Exception e) {
            ServicesLogger.LOGGER.error("Error validating invitation request", e);
            return new InvitationResult(request.getEmail(), request.getUserId(), false,
                "Internal error: " + e.getMessage(), "INTERNAL_ERROR");
        }
    }



    /**
     * Validate existing user invitation without sending email
     */
    private InvitationResult validateExistingUserInvitation(String userId) {
        UserModel user = session.users().getUserById(realm, userId);

        if (user == null) {
            return new InvitationResult(null, userId, false,
                "User does not exist", "USER_NOT_FOUND");
        }

        if (organization.isMember(user)) {
            return new InvitationResult(user.getEmail(), userId, false,
                "User already a member of the organization", "ALREADY_MEMBER");
        }

        return new InvitationResult(user.getEmail(), userId, true, "Ready for invitation");
    }




    /**
     * Send bulk invitations with single SMTP connection
     */
    private List<InvitationResult> sendBulkInvitations(List<ProcessedInvitation> validInvitations) {
        List<InvitationResult> results = new ArrayList<>();
        EmailTemplateProvider emailProvider = null;
        
        try {
            // Initialize email provider once
            emailProvider = session.getProvider(EmailTemplateProvider.class).setRealm(realm);
            
            for (ProcessedInvitation processedInvitation : validInvitations) {
                InvitationResult result = processedInvitation.getResult();
                InvitationRequest request = processedInvitation.getRequest();
                
                try {
                    UserModel user = getUserForInvitation(request);
                    String link = createInvitationLinkForUser(user);
                    
                    // Send email using the same provider instance
                    emailProvider.setUser(user)
                            .sendOrgInviteEmail(organization, link, TimeUnit.SECONDS.toMinutes(getActionTokenLifespan()));
                    
                    // Update result with success
                    String successMessage = user.getId() == null ? 
                        "Registration invitation sent successfully" : 
                        "Invitation sent successfully";
                    
                    results.add(new InvitationResult(result.getEmail(), result.getUserId(), true, successMessage));
                    
                } catch (EmailException e) {
                    ServicesLogger.LOGGER.error("Failed to send invitation email to: " + result.getEmail(), e);
                    results.add(new InvitationResult(result.getEmail(), result.getUserId(), false, 
                        "Failed to send invitation email: " + e.getMessage(), "EMAIL_SEND_FAILED"));
                } catch (Exception e) {
                    ServicesLogger.LOGGER.error("Error processing invitation for: " + result.getEmail(), e);
                    results.add(new InvitationResult(result.getEmail(), result.getUserId(), false, 
                        "Internal error: " + e.getMessage(), "INTERNAL_ERROR"));
                }
            }
            
        } catch (Exception e) {
            ServicesLogger.LOGGER.error("Failed to initialize email provider for bulk invitations", e);
            // If we can't initialize the email provider, mark all as failed
            for (ProcessedInvitation processedInvitation : validInvitations) {
                InvitationResult result = processedInvitation.getResult();
                results.add(new InvitationResult(result.getEmail(), result.getUserId(), false, 
                    "Failed to initialize email system", "EMAIL_SYSTEM_ERROR"));
            }
        }
        
        return results;
    }



    /**
     * Get or create user model for invitation
     */
    private UserModel getUserForInvitation(InvitationRequest request) {
        // Handle existing user by ID
        if (!StringUtil.isBlank(request.getUserId())) {
            return session.users().getUserById(realm, request.getUserId());
        }
        
        // Handle user by email
        UserModel user = session.users().getUserByEmail(realm, request.getEmail());
        
        if (user != null) {
            return user;
        }
        
        // Create temporary user for registration invitation
        user = new InMemoryUserAdapter(session, realm, null);
        user.setEmail(request.getEmail());
        
        if (request.getFirstName() != null && request.getLastName() != null) {
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
        }
        
        return user;
    }


    /**
     * Create invitation link for user
     */
    private String createInvitationLinkForUser(UserModel user) {
        return user.getId() == null ? createRegistrationLink(user) : createInvitationLink(user);
    }

}
