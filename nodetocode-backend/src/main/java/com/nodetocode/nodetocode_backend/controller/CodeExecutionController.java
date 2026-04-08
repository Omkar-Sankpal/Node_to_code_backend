package com.nodetocode.nodetocode_backend.controller;

import com.nodetocode.nodetocode_backend.dto.Judge0SubmissionResponse;
import com.nodetocode.nodetocode_backend.service.Judge0Service;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "http://localhost:5173",
        allowCredentials = "true",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS}
)
public class CodeExecutionController {

    private final Judge0Service judge0Service;

    @PostMapping("/execute")
    public ResponseEntity<Judge0SubmissionResponse> execute(
            @RequestBody ExecuteRequest request
    ) {

        Judge0SubmissionResponse response =
                judge0Service.executeCode(
                        request.getLanguageId(),
                        request.getCode(),
                        request.getStdin()
                );

        return ResponseEntity.ok(response);
    }

    @Data
    public static class ExecuteRequest {
        private Integer languageId;
        private String code;
        private String stdin;
    }
}