package com.trackswiftly.keycloak_userservice.dtos;

import java.util.ArrayList;
import java.util.List;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@AllArgsConstructor
@Builder
@Data
public class BulkInvitationResponse {
    

    public BulkInvitationResponse() {
        this.results = new ArrayList<>();
    }


    private int totalRequested;
    private int successCount;
    private int failureCount;
    private List<InvitationResult> results;



    public void addResult(InvitationResult result) {
        this.results.add(result);
        if (result.isSuccess()) {
            this.successCount++;
        } else {
            this.failureCount++;
        }
    }
}
