package com.trackswiftly.user_acls;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserACLService {
    private static final Map<String, Map<String, Map<String, List<String>>>> userAcls = new HashMap<>();

    static {
        userAcls.put("user_123", new HashMap<>() {{
            put("mqtt", Map.of("methods", List.of("subscribe")));
            put("gw/devices", Map.of("methods", List.of("GET" , "DELETE"), "ids", List.of("6004570", "6005024", "6005303", "6005305", "6005313", "6005347", "6005359", "6005376", "6005387", "6005396", "6005398", "6005515" )));
            put("gw/geofences", Map.of("methods", List.of("GET")));
            put("gw/calcs", Map.of("methods", List.of("GET")));
        }});
    }


    public static Map<String, Map<String, List<String>>> getAcl(String userId) {
        return userAcls.getOrDefault(userId, new HashMap<>());
    }
}
