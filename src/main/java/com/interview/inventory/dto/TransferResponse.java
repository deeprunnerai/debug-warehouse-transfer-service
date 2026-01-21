package com.interview.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    private Long transferId;
    private String sku;
    private String fromLocation;
    private String toLocation;
    private Integer quantity;
    private String status;
    private String message;
}
