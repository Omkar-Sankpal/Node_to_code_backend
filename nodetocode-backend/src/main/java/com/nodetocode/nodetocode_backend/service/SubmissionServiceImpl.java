package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.dao.ProblemDao;
import com.nodetocode.nodetocode_backend.dao.SubmissionDao;
import com.nodetocode.nodetocode_backend.dao.UserDao;
import com.nodetocode.nodetocode_backend.model.CodeLanguage;
import com.nodetocode.nodetocode_backend.model.Problem;
import com.nodetocode.nodetocode_backend.model.Submission;
import com.nodetocode.nodetocode_backend.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionDao submissionDao;
    private final UserDao userDao;
    private final ProblemDao problemDao;

    @Override
    public Submission saveSubmission(Long userId, Long problemId, CodeLanguage language, String code) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Problem problem = problemDao.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        // Upsert: update existing or create new
        Submission submission = submissionDao
                .findByUserIdAndProblemIdAndLanguage(userId, problemId, language)
                .orElse(Submission.builder()
                        .user(user)
                        .problem(problem)
                        .language(language)
                        .build());

        submission.setCode(code);
        return submissionDao.save(submission);
    }

    @Override
    public Optional<Submission> getSubmission(Long userId, Long problemId, CodeLanguage language) {
        return submissionDao.findByUserIdAndProblemIdAndLanguage(userId, problemId, language);
    }

    @Override
    public List<Submission> getSubmissionsForProblem(Long userId, Long problemId) {
        return submissionDao.findByUserIdAndProblemId(userId, problemId);
    }

    @Override
    public List<Submission> getAllSubmissions(Long userId) {
        return submissionDao.findByUserId(userId);
    }

    @Override
    public void markSolved(Long userId, Long problemId, CodeLanguage language, String code) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Problem problem = problemDao.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        Submission submission = submissionDao
                .findByUserIdAndProblemIdAndLanguage(userId, problemId, language)
                .orElse(Submission.builder()
                        .user(user)
                        .problem(problem)
                        .language(language)
                        .code(code)
                        .build());

        submission.setCode(code);
        submission.setSolved(true);
        submissionDao.save(submission);
    }

    @Override
    public List<Long> getSolvedProblemIds(Long userId) {
        return submissionDao.findSolvedProblemIdsByUserId(userId);
    }
}
