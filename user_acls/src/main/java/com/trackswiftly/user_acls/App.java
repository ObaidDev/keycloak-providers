package com.trackswiftly.user_acls;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.trackswiftly.utils.base.services.ACLManager;

/**
 * Hello world!
 */
public class App {


    static Logger logger = Logger.getLogger(App.class.getName());


    public static void main(String[] args) {
        String userId = "user_123";
        // 
        ACLManager aclManager = new ACLManager();

        Map<String, Map<String, Set<String>>> acl = aclManager.convertToSet(UserACLService.getAcl(userId));

        // ✅ User requests access to multiple items - All exist in ACL
        testAccess(aclManager, acl , "gw/devices", "DELETE", Arrays.asList("c55baac0-43b0-4ade-8db2-c4b14a34d74b" , "17196ab8-daf6-4af0-8ecf-73854ffad061"));

        // ❌ User requests access to multiple items - Some IDs are missing
        testAccess(aclManager,acl , "gw/devices", "GET", Arrays.asList("6004481", "6004439", "6003329"));

        // ✅ User tries to access "gw/geofences" with GET (No ID restriction)
        testAccess(aclManager,acl , "gw/geofences", "GET", Arrays.asList("any_id"));

        aclManager.testEncodeDecode(acl);
    }


    private static void testAccess(ACLManager aclManager , Map<String, Map<String, Set<String>>> acl, String uri, String method, List<String> itemIds) {
        long startTime = System.nanoTime();  // Capture start time

        boolean access = aclManager.hasAccess(acl , uri, method, itemIds);

        long endTime = System.nanoTime();  // Capture end time
        long duration = endTime - startTime;  // Calculate execution time

        if (logger.isLoggable(java.util.logging.Level.INFO)) {
            logger.info(String.format("Access to %s with %s (IDs: %s) → %s (Execution Time: %d ns)", uri, method, itemIds, access ? "✅ GRANTED" : "❌ DENIED", duration));
        }
    }
}
