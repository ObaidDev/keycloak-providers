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

        RealmModel realm = session.getContext().getRealm();

        OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
        OrganizationModel organization = provider.getById("eadeaa80-5b4e-45c5-ba99-224cdf81cb87") ;
        
        return new TrackSwiftlyResource(session , organization);
    }


    
}