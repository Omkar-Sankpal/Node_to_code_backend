// src/main/java/com/nodetocode/nodetocode_backend/service/UserService.java
package com.nodetocode.nodetocode_backend.service;

import com.nodetocode.nodetocode_backend.model.User;

public interface UserService {

    User findByEmail(String email);

    User createGoogleUser(String name, String email, String providerId);

    User processGoogleLogin(String name, String email, String providerId);

    User createGuestUser(String name, String email, String providerId);

    User getCurrentUser();

    void saveApiKey(User user, String apiKey);

    String getApiKey(User user);
}
