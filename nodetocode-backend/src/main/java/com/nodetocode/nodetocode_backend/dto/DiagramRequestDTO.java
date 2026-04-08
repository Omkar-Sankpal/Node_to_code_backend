package com.nodetocode.nodetocode_backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagramRequestDTO {
    private String diagramType; // UML | USE_CASE | ENTITY_RELATIONSHIP
    private String contextType; // PROJECT | PROBLEM
    private String title;
    private String description;
    private String language;
    private String code;
}
