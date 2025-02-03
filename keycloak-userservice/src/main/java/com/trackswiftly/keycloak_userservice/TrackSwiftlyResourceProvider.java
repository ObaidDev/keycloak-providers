package com.trackswiftly.keycloak_userservice;

import org.keycloak.models.KeycloakSession;
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