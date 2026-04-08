package com.nodetocode.nodetocode_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EdgeDTO {

    private String reactFlowId;
    private String sourceReactFlowId;
    private String targetReactFlowId;
    private String metadata; // JSON string: { type, data, markerEnd, style, animated }
}
