package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.dao.DSAProblemDao;
import com.nodetocode.nodetocode_backend.dao.DSASubmissionDao;
import com.nodetocode.nodetocode_backend.dao.UserDao;
import com.nodetocode.nodetocode_backend.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DSASubmissionServiceImpl implements DSASubmissionService {

    private final DSASubmissionDao dsaSubmissionDao;
    private final UserDao userDao;
    private final DSAProblemDao dsaProblemDao;

    @Override
    public DSASubmission saveSubmission(Long userId, Long dsaProblemId, CodeLanguage language, String code) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        DSAProblem problem = dsaProblemDao.findById(dsaProblemId)
                .orElseThrow(() -> new RuntimeException("DSA Problem not found"));

        DSASubmission submission = dsaSubmissionDao
                .findByUserIdAndDsaProblemIdAndLanguage(userId, dsaProblemId, language)
                .orElse(DSASubmission.builder()
                        .user(user)
                        .dsaProblem(problem)
                        .language(language)
                        .build());

        submission.setCode(code);
        return dsaSubmissionDao.save(submission);
    }

    @Override
    public Optional<DSASubmission> getSubmission(Long userId, Long dsaProblemId, CodeLanguage language) {
        return dsaSubmissionDao.findByUserIdAndDsaProblemIdAndLanguage(userId, dsaProblemId, language);
    }

    @Override
    public List<DSASubmission> getSubmissionsForProblem(Long userId, Long dsaProblemId) {
        return dsaSubmissionDao.findByUserIdAndDsaProblemId(userId, dsaProblemId);
    }

    @Override
    public List<DSASubmission> getAllSubmissions(Long userId) {
        return dsaSubmissionDao.findByUserId(userId);
    }

    @Override
    public void markSolved(Long userId, Long dsaProblemId, CodeLanguage language, String code) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        DSAProblem problem = dsaProblemDao.findById(dsaProblemId)
                .orElseThrow(() -> new RuntimeException("DSA Problem not found"));

        DSASubmission submission = dsaSubmissionDao
                .findByUserIdAndDsaProblemIdAndLanguage(userId, dsaProblemId, language)
                .orElse(DSASubmission.builder()
                        .user(user)
                        .dsaProblem(problem)
                        .language(language)
                        .code(code)
                        .build());

        submission.setCode(code);
        submission.setSolved(true);
        dsaSubmissionDao.save(submission);
    }

    @Override
    public List<Long> getSolvedProblemIds(Long userId) {
        return dsaSubmissionDao.findSolvedDsaProblemIdsByUserId(userId);
    }
}
