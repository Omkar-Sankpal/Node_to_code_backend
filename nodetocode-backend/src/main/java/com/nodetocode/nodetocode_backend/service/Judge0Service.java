package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.dto.Judge0SubmissionRequest;
import com.nodetocode.nodetocode_backend.dto.Judge0SubmissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class Judge0Service {

    private final RestTemplate restTemplate;

    private static final String JUDGE0_URL =
            "https://ce.judge0.com/submissions?wait=true&base64_encoded=false";

    public Judge0SubmissionResponse executeCode(
            Integer languageId,
            String sourceCode,
            String stdin
    ) {

        Judge0SubmissionRequest request = new Judge0SubmissionRequest();
        request.setLanguage_id(languageId);
        request.setSource_code(sourceCode);
        request.setStdin(stdin);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Judge0SubmissionRequest> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<Judge0SubmissionResponse> response =
                restTemplate.postForEntity(
                        JUDGE0_URL,
                        entity,
                        Judge0SubmissionResponse.class
                );

        return response.getBody();
    }
}