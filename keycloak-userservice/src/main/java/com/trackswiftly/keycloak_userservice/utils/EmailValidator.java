package com.trackswiftly.keycloak_userservice.utils;

import com.trackswiftly.keycloak_userservice.dtos.InvitationRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class EmailValidator {
    
    // Email regex pattern based on RFC 5322 but simplified for practical use
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    
    /**
     * Validates a single email address
     * @param email the email address to validate
     * @return true if the email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        email = email.trim();
        
        // Basic checks
        if (!email.contains("@")) {
            return false;
        }
        
        String[] parts = email.split("@");
        if (parts.length != 2) {
            return false;
        }
        
        String localPart = parts[0];
        String domainPart = parts[1];
        
        // Check local part (before @)
        if (!isValidLocalPart(localPart)) {
            return false;
        }
        
        // Check domain part (after @)
        if (!isValidDomainPart(domainPart)) {
            return false;
        }
        
        // Use regex for final validation
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validates the local part of an email (part before @)
     * @param localPart the local part to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidLocalPart(String localPart) {
        if (localPart.isEmpty()) {
            return false;
        }
        
        // Cannot start or end with a dot
        if (localPart.startsWith(".") || localPart.endsWith(".")) {
            return false;
        }
        
        // Cannot have consecutive dots
        if (localPart.contains("..")) {
            return false;
        }
        
        // Check length (local part should not exceed 64 characters per RFC 5321)
        if (localPart.length() > 64) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates the domain part of an email (part after @)
     * @param domainPart the domain part to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidDomainPart(String domainPart) {
        if (domainPart.isEmpty()) {
            return false;
        }
        
        // Must contain at least one dot
        if (!domainPart.contains(".")) {
            return false;
        }
        
        // Cannot start or end with a dot or hyphen
        if (domainPart.startsWith(".") || domainPart.endsWith(".") || 
            domainPart.startsWith("-") || domainPart.endsWith("-")) {
            return false;
        }
        
        // Cannot have consecutive dots
        if (domainPart.contains("..")) {
            return false;
        }
        
        // Check length (domain should not exceed 253 characters per RFC 5321)
        if (domainPart.length() > 253) {
            return false;
        }
        
        // Split by dots and validate each label
        String[] labels = domainPart.split("\\.");
        for (String label : labels) {
            if (!isValidDomainLabel(label)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validates a single domain label (part between dots in domain)
     * @param label the domain label to validate
     * @return true if valid, false otherwise
     */
    private static boolean isValidDomainLabel(String label) {
        if (label.isEmpty() || label.length() > 63) {
            return false;
        }
        
        // Cannot start or end with hyphen
        if (label.startsWith("-") || label.endsWith("-")) {
            return false;
        }
        
        // Should contain only alphanumeric characters and hyphens
        return label.matches("^[a-zA-Z0-9-]+$");
    }
    
    /**
     * Validates a list of invitation requests
     * @param userInvitations the list of invitation requests to validate
     * @return ValidationResult containing validation status and any errors
     */
    public static ValidationResult validateInvitationRequests(List<InvitationRequest> userInvitations) {
        ValidationResult result = new ValidationResult();
        
        if (userInvitations == null || userInvitations.isEmpty()) {
            result.addError("userInvitations", "User invitations list cannot be null or empty");
            return result;
        }
        
        for (int i = 0; i < userInvitations.size(); i++) {
            InvitationRequest invitation = userInvitations.get(i);
            String fieldPrefix = "userInvitations[" + i + "]";
            
            validateSingleInvitationRequest(invitation, fieldPrefix, result);
        }
        
        return result;
    }
    
    /**
     * Validates a single invitation request
     * @param invitation the invitation request to validate
     * @param fieldPrefix prefix for error field names
     * @param result the validation result to add errors to
     */
    private static void validateSingleInvitationRequest(InvitationRequest invitation, String fieldPrefix, ValidationResult result) {
        if (invitation == null) {
            result.addError(fieldPrefix, "Invitation request cannot be null");
            return;
        }
        
        // Validate email (required)
        if (invitation.getEmail() == null || invitation.getEmail().trim().isEmpty()) {
            result.addError(fieldPrefix + ".email", "Email cannot be null or empty");
        } else if (!isValidEmail(invitation.getEmail().trim())) {
            result.addError(fieldPrefix + ".email", "Email format is invalid");
        }
        
        // Validate firstName (optional, but if provided should not be empty)
        if (invitation.getFirstName() != null && invitation.getFirstName().trim().isEmpty()) {
            result.addError(fieldPrefix + ".firstName", "First name cannot be empty if provided");
        }
        
        // Validate lastName (optional, but if provided should not be empty)
        if (invitation.getLastName() != null && invitation.getLastName().trim().isEmpty()) {
            result.addError(fieldPrefix + ".lastName", "Last name cannot be empty if provided");
        }
        
        // Validate userId (optional, but if provided should not be empty)
        if (invitation.getUserId() != null && invitation.getUserId().trim().isEmpty()) {
            result.addError(fieldPrefix + ".userId", "User ID cannot be empty if provided");
        }
    }
    
    /**
     * Helper class to hold validation results
     */
    public static class ValidationResult {
        private final Map<String, String> errors = new HashMap<>();
        
        public void addError(String field, String message) {
            errors.put(field, message);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public Map<String, String> getErrors() {
            return errors;
        }
        
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
        
        public int getErrorCount() {
            return errors.size();
        }
    }
}