package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.model.CodeLanguage;
import com.nodetocode.nodetocode_backend.model.DSASubmission;

import java.util.List;
import java.util.Optional;

public interface DSASubmissionService {

    DSASubmission saveSubmission(Long userId, Long dsaProblemId, CodeLanguage language, String code);

    Optional<DSASubmission> getSubmission(Long userId, Long dsaProblemId, CodeLanguage language);

    List<DSASubmission> getSubmissionsForProblem(Long userId, Long dsaProblemId);

    List<DSASubmission> getAllSubmissions(Long userId);

    void markSolved(Long userId, Long dsaProblemId, CodeLanguage language, String code);

    List<Long> getSolvedProblemIds(Long userId);
}
