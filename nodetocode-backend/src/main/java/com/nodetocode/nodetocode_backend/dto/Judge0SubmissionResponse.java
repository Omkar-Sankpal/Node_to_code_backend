package com.nodetocode.nodetocode_backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Judge0SubmissionResponse {

    private String stdout;
    private String stderr;
    private String compile_output;
    private String message;

    // Judge0 returns time as "0.993" (String decimal)
    private Double time;

    private Integer memory;

    private Status status;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        private Integer id;
        private String description;
    }
}