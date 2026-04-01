package com.smartloan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerLookupResponse {
    private String id;
    private String email;
    private String name;
    private Double trustScore;
    private Boolean found;
    private String message;
}
