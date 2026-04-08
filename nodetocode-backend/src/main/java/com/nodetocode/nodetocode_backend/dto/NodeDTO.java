package com.nodetocode.nodetocode_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeDTO {

    private String reactFlowId;
    private String type;
    private double positionX;
    private double positionY;
    private String prompt;
    private String metadata; // JSON string of React Flow node.data (minus callbacks)
}
