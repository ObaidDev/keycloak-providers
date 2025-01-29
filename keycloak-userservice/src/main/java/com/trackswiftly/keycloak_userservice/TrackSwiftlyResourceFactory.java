package com.trackswiftly.keycloak_userservice;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class TrackSwiftlyResourceFactory implements RealmResourceProviderFactory{


    public static final String PROVIDER_ID = "users-services";

    @Override
    public void close() {
        /**
         * 
         */
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
       return new TrackSwiftlyResourceProvider(session) ;
    }

    @Override
    public String getId() {

        return PROVIDER_ID ;
    }

    @Override
    public void init(Scope arg0) {
        /*
         * 
         * 
         */

    }

    @Override
    public void postInit(KeycloakSessionFactory arg0) {
        /*
         * 
         * 
         */
    }

    
}
