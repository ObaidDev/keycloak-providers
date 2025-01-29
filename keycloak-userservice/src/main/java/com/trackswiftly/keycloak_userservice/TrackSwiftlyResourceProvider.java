package com.trackswiftly.keycloak_userservice;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.services.resource.RealmResourceProvider;





public class TrackSwiftlyResourceProvider implements RealmResourceProvider{

    private KeycloakSession session;


	public TrackSwiftlyResourceProvider(KeycloakSession session) {

        this.session = session;
    }

    @Override
    public void close() {
        /***
         * 
         * 
         */
    }

    @Override
    public Object getResource() {
        
        return new TrackSwiftlyResource(session );
    }


    
}