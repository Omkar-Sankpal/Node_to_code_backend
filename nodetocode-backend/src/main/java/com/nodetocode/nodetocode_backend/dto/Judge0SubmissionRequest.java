package com.nodetocode.nodetocode_backend.dto;

import lombok.Data;

@Data
public class Judge0SubmissionRequest {

    private Integer language_id;
    private String source_code;
    private String stdin;
}