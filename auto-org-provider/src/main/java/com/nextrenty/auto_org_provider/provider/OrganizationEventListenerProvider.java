package com.nextrenty.auto_org_provider.provider;

import java.util.Map;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.models.GroupModel;



public class OrganizationEventListenerProvider implements EventListenerProvider{

    private final KeycloakSession session;

    public OrganizationEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
        /****
         * 
         */
    }

    @Override
    public void onEvent(Event event) {
        
        if (event.getType().equals(EventType.REGISTER)) {

            RealmModel realm = session.realms().getRealm(event.getRealmId());
            UserModel user = session.users().getUserById(realm, event.getUserId());

            /*
             * join it to the admin group
             */

            if (isDirectRegistration(event)) {


                // Join the user to the admin group
                GroupModel group = session.groups().getGroupByName(realm, null, "ADMIN_GROUP");
                user.joinGroup(group);
    
                // Create an organization for the user
                createOrganizationForUser(realm, user);
            }
        }
    }

    @Override
    public void onEvent(AdminEvent arg0, boolean arg1) {
        /****
         * 
         */
    }
    



    private void createOrganizationForUser(RealmModel realm, UserModel user) {
       
        OrganizationProvider organizationProvider = session.getProvider(OrganizationProvider.class);

        OrganizationModel organization = organizationProvider.create(
                                            user.getUsername() + "-organization", 
                                            user.getUsername() +"-org-alias"
                                        );
        
        organizationProvider.addManagedMember(organization, user);

    }


    private boolean isDirectRegistration(Event event) {
        
        Map<String, String> details = event.getDetails();

        return !(details.containsKey("org_id") && "ORGIVT".equals(details.get("action"))) ;

    }
}
