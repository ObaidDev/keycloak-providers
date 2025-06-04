package com.trackswiftly.keycloak_userservice.utils;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;


public class CorsUtils {


    private CorsUtils() {
        // Private constructor to prevent instantiation
    }



    public static Response addCorsHeaders(Response originalResponse, HttpHeaders headers) {
        String origin = getOriginFromHeaders(headers);
        
        return Response.fromResponse(originalResponse)
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", 
                    "Content-Type, Authorization, X-Requested-With, Accept, Origin")
                .header("Access-Control-Expose-Headers", "Location")
                .header("Access-Control-Allow-Credentials", "true")
                .build();
    }


    private static String getOriginFromHeaders(HttpHeaders headers) {
        String origin = headers.getHeaderString("Origin");
        
        // You can add origin validation here if needed
        if (origin == null || origin.trim().isEmpty()) {
            return "*"; // Fallback, but be careful with credentials
        }
        
        // Optional: Validate against allowed origins from realm settings
        if (isOriginAllowedEnhanced(origin)) {
            return origin;
        }
        
        return "*"; // or throw an exception for unauthorized origins
    }




    private static boolean isOriginAllowedEnhanced(String origin) {
        if (origin == null || origin.trim().isEmpty()) {
            return false;
        }
        
        String lowerOrigin = origin.toLowerCase();
        
        try {
            java.net.URL url = new java.net.URL(origin);
            String hostname = url.getHost().toLowerCase();
            
            // Check for trackswiftly domains
            if (hostname.contains("trackswiftly")) {
                return true;
            }
            
            // Check for localhost variations
            if (isLocalhostVariation(hostname)) {
                return true;
            }
            
            // Check for common development domains
            return isDevelopmentDomain(hostname);
            
        } catch (java.net.MalformedURLException e) {
            return fallbackOriginCheck(lowerOrigin);
        }
    }




    private static boolean isLocalhostVariation(String hostname) {
        return hostname.equals("localhost") || 
            hostname.equals("127.0.0.1") || 
            hostname.equals("0.0.0.0") ||
            hostname.equals("::1") || // IPv6 localhost
            hostname.endsWith(".localhost") || // subdomain.localhost
            hostname.matches("192\\.168\\.\\d+\\.\\d+") || // Local network
            hostname.matches("10\\.\\d+\\.\\d+\\.\\d+") || // Local network
            hostname.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\.\\d+\\.\\d+"); // Local network
    }

    // Helper method for development domains
    private static boolean isDevelopmentDomain(String hostname) {
        return hostname.endsWith(".local") ||
            hostname.endsWith(".dev") ||
            hostname.endsWith(".test") ||
            hostname.contains("staging") ||
            hostname.contains("dev");
    }

    // Fallback method when URL parsing fails
    private static boolean fallbackOriginCheck(String lowerOrigin) {
        return lowerOrigin.contains("trackswiftly") || 
            lowerOrigin.contains("localhost") ||
            lowerOrigin.contains("127.0.0.1") ||
            lowerOrigin.contains("0.0.0.0") ||
            lowerOrigin.contains("::1");
    }

    
}
