package com.trackswiftly.keycloak_userservice.dtos;

public enum TrackSwiftlyRoles {
    
    /***
     * Manages their organization
     * Views all orders within their organization
     * Full access to org reports and analytics
     */

    ADMIN  ,

    /**
     * Views all orders in their department
     * Access to department analytics
     * Manages team workload
     */
    MANAGER ,


    /***
     * Creates/edits orders
     * Assigns drivers
     * Views all active orders
     * Manages scheduling
     */
    DISPATCHER ,

    /***
     * Views their assigned orders only
     * Updates delivery status
     * Manages their schedule
     * Access to mobile app features
     */
    DRIVER ,
}
