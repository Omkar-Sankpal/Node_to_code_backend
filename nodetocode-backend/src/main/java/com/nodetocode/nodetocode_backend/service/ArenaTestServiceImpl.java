package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.dao.ArenaTestDao;
import com.nodetocode.nodetocode_backend.dao.DSAProblemDao;
import com.nodetocode.nodetocode_backend.dao.UserDao;
import com.nodetocode.nodetocode_backend.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArenaTestServiceImpl implements ArenaTestService {

    private final ArenaTestDao arenaTestDao;
    private final DSAProblemDao dsaProblemDao;
    private final UserDao userDao;

    /* ── Duration per mode (seconds) ── */
    private int durationFor(ArenaTestMode mode, Difficulty difficulty) {
        return switch (mode) {
            case RANDOM_CHALLENGE -> switch (difficulty) {
                case EASY -> 15 * 60;
                case MEDIUM -> 20 * 60;
                case HARD -> 30 * 60;
            };
            case SPRINT_TEST -> 60 * 60;
            case FULL_TEST -> 120 * 60;
        };
    }

    private int problemCount(ArenaTestMode mode) {
        return switch (mode) {
            case RANDOM_CHALLENGE -> 1;
            case SPRINT_TEST -> 2;
            case FULL_TEST -> 4;
        };
    }

    @Override
    public ArenaTest startTest(Long userId, ArenaTestMode mode) {
        // Prevent starting a new test while one is active
        arenaTestDao.findByUserIdAndActiveTrue(userId).ifPresent(t -> {
            throw new RuntimeException("You already have an active test. Finish it first.");
        });

        User user = userDao.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        int count = problemCount(mode);
        List<DSAProblem> allProblems = dsaProblemDao.findAll();
        if (allProblems.size() < count) {
            throw new RuntimeException("Not enough problems in the database to create this test.");
        }

        // For RANDOM_CHALLENGE, pick 1 and determine duration by difficulty
        // For others, pick a mix
        List<DSAProblem> selected;
        int duration;

        if (mode == ArenaTestMode.RANDOM_CHALLENGE) {
            Collections.shuffle(allProblems);
            selected = List.of(allProblems.get(0));
            duration = durationFor(mode, selected.get(0).getDifficulty());
        } else {
            // Try to pick a balanced mix
            List<DSAProblem> easy = allProblems.stream().filter(p -> p.getDifficulty() == Difficulty.EASY).collect(Collectors.toList());
            List<DSAProblem> medium = allProblems.stream().filter(p -> p.getDifficulty() == Difficulty.MEDIUM).collect(Collectors.toList());
            List<DSAProblem> hard = allProblems.stream().filter(p -> p.getDifficulty() == Difficulty.HARD).collect(Collectors.toList());
            Collections.shuffle(easy);
            Collections.shuffle(medium);
            Collections.shuffle(hard);

            selected = new ArrayList<>();
            if (mode == ArenaTestMode.SPRINT_TEST) {
                // 1 easy/medium + 1 medium/hard
                if (!easy.isEmpty()) selected.add(easy.remove(0));
                else if (!medium.isEmpty()) selected.add(medium.remove(0));
                if (!hard.isEmpty()) selected.add(hard.remove(0));
                else if (!medium.isEmpty()) selected.add(medium.remove(0));
                else if (!easy.isEmpty()) selected.add(easy.remove(0));
            } else {
                // FULL_TEST: 1 easy + 2 medium + 1 hard
                if (!easy.isEmpty()) selected.add(easy.remove(0));
                for (int i = 0; i < 2 && !medium.isEmpty(); i++) selected.add(medium.remove(i < medium.size() ? 0 : 0));
                if (!hard.isEmpty()) selected.add(hard.remove(0));
            }

            // Fill remaining slots randomly
            List<DSAProblem> remaining = new ArrayList<>(allProblems);
            remaining.removeAll(selected);
            Collections.shuffle(remaining);
            while (selected.size() < count && !remaining.isEmpty()) {
                selected.add(remaining.remove(0));
            }

            duration = durationFor(mode, Difficulty.EASY); // uses mode default
        }

        String ids = selected.stream().map(p -> String.valueOf(p.getId())).collect(Collectors.joining(","));

        ArenaTest test = ArenaTest.builder()
                .user(user)
                .mode(mode)
                .problemIds(ids)
                .durationSeconds(duration)
                .totalProblems(selected.size())
                .build();

        return arenaTestDao.save(test);
    }

    @Override
    public ArenaTest finishTest(Long userId, Long testId, List<Long> solvedProblemIds) {
        ArenaTest test = arenaTestDao.findByIdAndUserId(testId, userId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        if (!test.getActive()) {
            throw new RuntimeException("This test has already been submitted.");
        }

        test.setFinishedAt(LocalDateTime.now());
        test.setActive(false);

        // Calculate solved count
        Set<Long> assignedIds = Arrays.stream(test.getProblemIds().split(","))
                .map(Long::parseLong).collect(Collectors.toSet());
        Set<Long> solvedSet = solvedProblemIds != null ? new HashSet<>(solvedProblemIds) : Set.of();
        int solved = (int) assignedIds.stream().filter(solvedSet::contains).count();
        test.setSolvedCount(solved);

        // Calculate time taken
        long secondsTaken = Duration.between(test.getStartedAt(), test.getFinishedAt()).getSeconds();
        long allowedSeconds = test.getDurationSeconds();

        // Score calculation:
        // Base: solved/total * 700
        // Time bonus: up to 300 points for finishing early
        int baseScore = test.getTotalProblems() > 0
                ? (int) ((double) solved / test.getTotalProblems() * 700)
                : 0;

        int timeBonus = 0;
        if (solved > 0 && secondsTaken < allowedSeconds) {
            double timeRatio = 1.0 - ((double) secondsTaken / allowedSeconds);
            timeBonus = (int) (timeRatio * 300);
        }

        int totalScore = baseScore + timeBonus;
        test.setScore(totalScore);
        test.setTimeBonus(timeBonus);

        // Badge assignment
        String badge;
        if (totalScore >= 900) badge = "PLATINUM";
        else if (totalScore >= 750) badge = "GOLD";
        else if (totalScore >= 500) badge = "SILVER";
        else if (totalScore >= 250) badge = "BRONZE";
        else badge = "NONE";
        test.setBadge(badge);

        return arenaTestDao.save(test);
    }

    @Override
    public ArenaTest getTest(Long userId, Long testId) {
        return arenaTestDao.findByIdAndUserId(testId, userId)
                .orElseThrow(() -> new RuntimeException("Test not found"));
    }

    @Override
    public List<ArenaTest> getHistory(Long userId) {
        return arenaTestDao.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public ArenaTest getActiveTest(Long userId) {
        return arenaTestDao.findByUserIdAndActiveTrue(userId).orElse(null);
    }

    @Override
    public Map<String, Object> getStats(Long userId) {
        long totalTests = arenaTestDao.countByUserIdAndActiveFalse(userId);
        int totalScore = arenaTestDao.sumScoreByUserId(userId);
        int bestScore = arenaTestDao.maxScoreByUserId(userId);

        // Badge counts
        List<ArenaTest> history = arenaTestDao.findByUserIdOrderByCreatedAtDesc(userId);
        long platinumCount = history.stream().filter(t -> "PLATINUM".equals(t.getBadge())).count();
        long goldCount = history.stream().filter(t -> "GOLD".equals(t.getBadge())).count();
        long silverCount = history.stream().filter(t -> "SILVER".equals(t.getBadge())).count();
        long bronzeCount = history.stream().filter(t -> "BRONZE".equals(t.getBadge())).count();

        // Average score
        double avgScore = totalTests > 0 ? (double) totalScore / totalTests : 0;

        // Streak: consecutive tests with score >= 500
        int streak = 0;
        for (ArenaTest t : history) {
            if (!t.getActive() && t.getScore() >= 500) streak++;
            else if (!t.getActive()) break;
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTests", totalTests);
        stats.put("totalScore", totalScore);
        stats.put("bestScore", bestScore);
        stats.put("avgScore", Math.round(avgScore));
        stats.put("streak", streak);
        stats.put("platinum", platinumCount);
        stats.put("gold", goldCount);
        stats.put("silver", silverCount);
        stats.put("bronze", bronzeCount);

        return stats;
    }
}
