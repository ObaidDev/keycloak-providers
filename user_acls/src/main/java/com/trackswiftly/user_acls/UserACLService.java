package com.trackswiftly.user_acls;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserACLService {
    private static final Map<String, Map<String, Map<String, List<String>>>> userAcls = new HashMap<>();

    static {
        userAcls.put("user_123", new HashMap<>() {{

            put("mqtt", Map.of("methods", List.of("subscribe", "publish")));
            put("gw/devices", Map.of("methods", List.of("GET", "POST", "PUT", "DELETE")));
            put("gw/geofences", Map.of("methods", List.of("GET", "POST", "PUT", "DELETE", "PATCH")));
            put("gw/calcs" , Map.of("methods", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")));
            put("gw/pois", Map.of("methods", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")));
        }});
    }


    public static Map<String, Map<String, List<String>>> getAcl(String userId) {
        return userAcls.getOrDefault(userId, new HashMap<>());
    }
}
