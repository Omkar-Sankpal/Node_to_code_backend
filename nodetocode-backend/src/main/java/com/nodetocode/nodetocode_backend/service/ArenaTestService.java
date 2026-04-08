package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.model.ArenaTest;
import com.nodetocode.nodetocode_backend.model.ArenaTestMode;

import java.util.List;
import java.util.Map;

public interface ArenaTestService {

    /** Start a new arena test – auto-assigns problems */
    ArenaTest startTest(Long userId, ArenaTestMode mode);

    /** Submit / finish an arena test – calculates score, badge, etc. */
    ArenaTest finishTest(Long userId, Long testId, List<Long> solvedProblemIds);

    /** Get a single test */
    ArenaTest getTest(Long userId, Long testId);

    /** Get all tests for a user (history) */
    List<ArenaTest> getHistory(Long userId);

    /** Get the currently active test (if any) */
    ArenaTest getActiveTest(Long userId);

    /** Get aggregated stats for profile page */
    Map<String, Object> getStats(Long userId);
}
