package com.trackswiftly.user_acls;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserACLService {
    private static final Map<String, Map<String, Map<String, List<String>>>> userAcls = new HashMap<>();

    static {
        userAcls.put("user_123", new HashMap<>() {{
            put("mqtt", Map.of("methods", List.of("subscribe")));
            put("gw/devices", Map.of("methods", List.of("GET" , "DELETE"), "ids", List.of("6fa3389c-19d1-49da-ab9b-a4fb41e2f109",
                        "c55baac0-43b0-4ade-8db2-c4b14a34d74b",
                        "17196ab8-daf6-4af0-8ecf-73854ffad061",
                        "4a9af167-95ed-4b93-87e3-20dccc1d7c67",
                        "b1751235-72aa-41a2-ac1c-abfdb761bfb4",
                        "2299492e-5bbc-4e83-9bb4-d6763a5983f8",
                        "b1185129-757d-47d2-9340-dcc2b3f7f86d",
                        "2d8e510e-b349-4889-a9d5-2ae8c1ba2ade",
                        "bb124eb2-be34-46cd-9ffc-73273feb35ce",
                        "670e9e11-e970-4227-9e70-7cb9c4b16f82",
                        "6b0b98a7-27e8-4880-afb2-a1ab9c662fd8",
                        "ad07003d-b1e1-4001-a002-ab288f74e776",
                        "279e2389-08ff-4b49-8bf6-0fe24c9cf9ff",
                        "498bd6f1-cc05-4fb0-a884-fe7e9590a92a",
                        "0c68d33e-eb40-4d48-8f50-96687ea8e8f8",
                        "62688579-40a2-4ca4-baa3-d11be7f5c462",
                        "b353e862-85b2-4818-b810-74face2a04fd",
                        "e3320969-62eb-491f-ab16-384d03cb92b8",
                        "9ad374f2-7ccb-43b9-906b-fdd52575cdc7",
                        "cc24443a-c3c1-4a0c-9edb-94ed38d210dd",
                        "2b327610-e1df-44a7-99c3-7fe03516bda6",
                        "bf85672c-68c8-4d75-9dc3-56461fea1784",
                        "7535c064-075a-4245-ab6c-a57cb305920c",
                        "f33ba8bd-64d9-4594-a784-cd5e36e48a3a",
                        "395734a8-bd76-43b5-b48b-35b97185fb85",
                        "a2929cd9-d1fb-45ac-8d9c-c44c9cf6dcac",
                        "250a8430-2be1-453a-92f0-373514155ef8",
                        "d914da26-9962-4d33-8290-e7bfc3823872",
                        "4badc9ca-e83c-4d98-ae55-d5052f996414",
                        "0d013f37-3bf1-4b68-9b57-1499ab55c9d7",
                        "181d806f-e253-41b9-8501-d3540497916b",
                        "8aa31bb7-8169-439a-b716-c418279fe1ac",
                        "aae272f5-a70d-47f3-a9b0-a4390cdf037d",
                        "ffce3db7-52cb-4bca-8ce3-baec58b15647",
                        "2031a48e-f94e-43ed-9ca6-4880d8d4b49f",
                        "479a0957-577a-4a2c-b14b-7b7f0b68e111",
                        "47726420-652d-41fa-bd64-298257e2bb77",
                        "3be904a6-27a2-4c07-8b05-be19610f2a15",
                        "27addcb0-2767-4b9d-84a8-1e460304781f",
                        "23cb70ac-1e9d-48de-b5e5-fd0d0659b057",
                        "b8b3fbe3-b787-4784-b3c1-449c1d6e4d58" )));
            put("gw/geofences", Map.of("methods", List.of("GET")));
            put("gw/calcs", Map.of("methods", List.of("GET")));
        }});
    }


    public static Map<String, Map<String, List<String>>> getAcl(String userId) {
        return userAcls.getOrDefault(userId, new HashMap<>());
    }
}
