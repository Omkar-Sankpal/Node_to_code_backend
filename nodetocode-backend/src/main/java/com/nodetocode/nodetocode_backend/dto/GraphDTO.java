package com.nodetocode.nodetocode_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphDTO {

    private List<NodeDTO> nodes;
    private List<EdgeDTO> edges;
    private String generatedPrompt; // populated on response only

    // React Flow viewport state
    private Double viewportX;
    private Double viewportY;
    private Double viewportZoom;
}
