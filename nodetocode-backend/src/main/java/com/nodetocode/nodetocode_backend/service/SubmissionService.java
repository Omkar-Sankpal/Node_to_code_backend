package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.model.CodeLanguage;
import com.nodetocode.nodetocode_backend.model.Submission;

import java.util.List;
import java.util.Optional;

public interface SubmissionService {

    Submission saveSubmission(Long userId, Long problemId, CodeLanguage language, String code);

    Optional<Submission> getSubmission(Long userId, Long problemId, CodeLanguage language);

    List<Submission> getSubmissionsForProblem(Long userId, Long problemId);

    List<Submission> getAllSubmissions(Long userId);

    void markSolved(Long userId, Long problemId, CodeLanguage language, String code);

    List<Long> getSolvedProblemIds(Long userId);
}
