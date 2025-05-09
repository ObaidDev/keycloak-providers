package com.trackswiftly.user_acls;

import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.logging.Logger;



public class ACLManager {

    private final Map<String, Map<String, Set<String>>> aclTable;

    static Logger log = Logger.getLogger(ACLManager.class.getName());

    public ACLManager(String userId) {
        // Convert List of IDs to HashSet for O(1) lookups
        this.aclTable = convertToSet(UserACLService.getAcl(userId));
    }


    private Map<String, Map<String, Set<String>>> convertToSet(Map<String, Map<String, List<String>>> rawAcl) {
        Map<String, Map<String, Set<String>>> optimizedAcl = new HashMap<>();
        for (var entry : rawAcl.entrySet()) {
            String uri = entry.getKey();
            Map<String, List<String>> permissions = entry.getValue();

            Map<String, Set<String>> optimizedPermissions = new HashMap<>();
            optimizedPermissions.put("methods", new HashSet<>(permissions.getOrDefault("methods", List.of())));
            optimizedPermissions.put("ids", new HashSet<>(permissions.getOrDefault("ids", List.of())));

            optimizedAcl.put(uri, optimizedPermissions);
        }
        return optimizedAcl;
    }


    public boolean hasAccess(String uri, String method, List<String> itemIds) {
        
        if (!aclTable.containsKey(uri)) {
            return false; // URI not found
        }

        Map<String, Set<String>> permissions = aclTable.get(uri);
        Set<String> allowedMethods = permissions.getOrDefault("methods", Set.of());

        if (!allowedMethods.contains(method)) {
            return false; // Method not allowed
        }

        Set<String> allowedIds = permissions.getOrDefault("ids", Set.of());

        // If no specific IDs are enforced, allow access
        if (allowedIds.isEmpty()) {
            return true;
        }

        // ✅ More Efficient Check Using `Set.containsAll()`
        return allowedIds.containsAll(itemIds);
    }



    public void testEncodeDecode(){

        String comporedAcl = CompressedAclService.compressAcl(aclTable);

        log.info("Compressed ACL: " + comporedAcl);

        Map<String, Map<String, Set<String>>> decompressedAcl = CompressedAclService.decompressAcl(comporedAcl);

        log.info("Decompressed ACL: " + decompressedAcl);
    }
}
