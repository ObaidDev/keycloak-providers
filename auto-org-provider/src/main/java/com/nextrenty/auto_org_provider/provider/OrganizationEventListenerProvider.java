package com.nextrenty.auto_org_provider.provider;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;

public class OrganizationEventListenerProvider implements EventListenerProvider{

    private final KeycloakSession session;

    public OrganizationEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Unimplemented method 'close'");
    }

    @Override
    public void onEvent(Event event) {
        
        if (event.getType().equals(EventType.REGISTER)) {
            RealmModel realm = session.realms().getRealm(event.getRealmId());
            UserModel user = session.users().getUserById(realm, event.getUserId());

            createOrganizationForUser(realm, user);
        }
    }

    @Override
    public void onEvent(AdminEvent arg0, boolean arg1) {
        throw new UnsupportedOperationException("Unimplemented method 'onEvent'");
    }
    



    private void createOrganizationForUser(RealmModel realm, UserModel user) {
       
        OrganizationProvider organizationProvider = session.getProvider(OrganizationProvider.class);

        OrganizationModel organization = organizationProvider.create(
                                            user.getUsername() + "-organization", 
                                            user.getUsername() +"-org-alias"
                                        );
        
        organizationProvider.addMember(organization, user);

    }
}
