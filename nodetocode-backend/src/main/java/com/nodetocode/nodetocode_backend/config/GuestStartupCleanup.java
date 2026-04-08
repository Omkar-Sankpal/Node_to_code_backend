package com.nodetocode.nodetocode_backend.config;

import com.nodetocode.nodetocode_backend.dao.UserDao;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class GuestStartupCleanup {

    private final UserDao userDao;

    @Scheduled(fixedRate = 30000000)
    @Transactional
    public void cleanupGuests() {

        // Keep guest data for 7 days (matching JWT cookie lifetime)
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        userDao.deleteByRoleAndCreatedAtBefore(
                "ROLE_GUEST",
                cutoff
        );

        System.out.println("Old guest users (>7 days) cleaned.");
    }
}