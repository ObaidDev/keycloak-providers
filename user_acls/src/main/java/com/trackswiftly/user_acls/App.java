package com.trackswiftly.user_acls;

import java.util.Arrays;
import java.util.List;


import java.util.logging.Logger;

/**
 * Hello world!
 */
public class App {


    static Logger logger = Logger.getLogger(App.class.getName());


    public static void main(String[] args) {
        String userId = "user_123";
        ACLManager aclManager = new ACLManager(userId);

        // ✅ User requests access to multiple items - All exist in ACL
        testAccess(aclManager, "gw/devices", "DELETE", Arrays.asList("6005021", "6005303", "6005305", "6005313", "6005347", "6005359", "6005376", "6005387", "6005396", "6005398", "6005515"));

        // ❌ User requests access to multiple items - Some IDs are missing
        testAccess(aclManager, "gw/devices", "GET", Arrays.asList("6004481", "6004439", "6003329"));

        // ✅ User tries to access "gw/geofences" with GET (No ID restriction)
        testAccess(aclManager, "gw/geofences", "GET", Arrays.asList("any_id"));
    }


    private static void testAccess(ACLManager aclManager, String uri, String method, List<String> itemIds) {
        long startTime = System.nanoTime();  // Capture start time

        boolean access = aclManager.hasAccess(uri, method, itemIds);

        long endTime = System.nanoTime();  // Capture end time
        long duration = endTime - startTime;  // Calculate execution time

        if (logger.isLoggable(java.util.logging.Level.INFO)) {
            logger.info(String.format("Access to %s with %s (IDs: %s) → %s (Execution Time: %d ns)", uri, method, itemIds, access ? "✅ GRANTED" : "❌ DENIED", duration));
        }
    }
}
