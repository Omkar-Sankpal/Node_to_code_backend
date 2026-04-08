// src/main/java/com/nodetocode/nodetocode_backend/service/UserServiceImpl.java
package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.dao.UserDao;
import com.nodetocode.nodetocode_backend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public User findByEmail(String email) {
        return userDao.findByEmail(email)
                .orElse(null);
    }

    @Override
    public User createGoogleUser(String name, String email, String providerId) {

        User user = new User();
        user.setName(name);
        user.setEmail(email);

        // Google users don’t use password
        user.setPassword("GOOGLE_USER");
        user.setRole("ROLE_USER");

        user.setVerified(true);
        user.setProvider("GOOGLE");
        user.setProviderId(providerId);
        user.setTotalTokensUsed(0L);

        return userDao.save(user);
    }

    @Override
    public User processGoogleLogin(String name, String email, String providerId) {

        Optional<User> existingUser =
                userDao.findByProviderAndProviderId("GOOGLE", providerId);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // If user exists with same email but LOCAL provider
        Optional<User> emailUser = userDao.findByEmail(email);
        if (emailUser.isPresent()) {
            return emailUser.get(); // You can decide to merge or block
        }

        // Create new Google user
        return createGoogleUser(name, email, providerId);
    }

    @Override
    public User createGuestUser(String name, String email, String providerId) {

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("GUEST_USER");
        user.setVerified(true);
        user.setProvider("GUEST");
        user.setProviderId(providerId);
        user.setRole("ROLE_GUEST");
        user.setTotalTokensUsed(0L);

        return userDao.save(user);
    }

    @Override
    public User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Unauthorized");
        }

        String email = authentication.getName();

        return userDao.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public void saveApiKey(User user, String apiKey) {
        user.setAiApiKey(apiKey);
        userDao.save(user);
    }

    @Override
    public String getApiKey(User user) {
        return user.getAiApiKey();
    }

}
