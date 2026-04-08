//// UserController.java
//package com.nodetocode.nodetocode_backend.controller;
//
//import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
//import com.nodetocode.nodetocode_backend.model.User;
//import com.nodetocode.nodetocode_backend.security.GoogleVerifier;
//import com.nodetocode.nodetocode_backend.security.JwtUtil;
//import com.nodetocode.nodetocode_backend.service.UserService;
//
//import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletResponse;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/users")
//@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
//
//public class UserController {
//
//    @Autowired
//    private UserService userService;
//
//    @PostMapping("/oauth/google")
//    public ResponseEntity<?> googleLogin(
//            @RequestBody Map<String, String> body,
//            HttpServletResponse response
//    ) {
//        try {
//
//            String token = body.get("token");
//
//            var payload = GoogleVerifier.verifyToken(token);
//
//            String email = payload.getEmail();
//            String name = (String) payload.get("name");
//            String providerId = payload.getSubject();
//
//            User user = userService.processGoogleLogin(name, email, providerId);
//
//            String jwt = JwtUtil.generateToken(
//                    user.getEmail(),
//                    user.getRole(),
//                    user.getProvider()
//            );
//
//            // Create secure cookie
//            Cookie cookie = new Cookie("jwt", jwt);
//            cookie.setHttpOnly(true);
////            cookie.setSecure(true); // true in production (https)
//            cookie.setSecure(false); // change krta hoon once i am in prod
//            cookie.setPath("/");
//            cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
//
//            response.addCookie(cookie);
//
//            return ResponseEntity.ok(Map.of(
//                    "message", "Login successful",
//                    "userId", user.getId(),
//                    "name", user.getName(),
//                    "email", user.getEmail(),
//                    "role", user.getRole(),
//                    "provider", user.getProvider()
//            ));
//
//        } catch (Exception e) {
//            return ResponseEntity
//                    .badRequest()
//                    .body(Map.of("error", "Invalid Google Token"));
//        }
//    }
//
//    @PostMapping("/guest")
//    public ResponseEntity<?> guestLogin(HttpServletResponse response) {
//
//        String guestId = UUID.randomUUID().toString();
//        String guestEmail = "guest_" + guestId + "@nodetocode.app";
//        String guestName = "Guest User";
//
//        User guestUser = userService.createGuestUser(
//                guestName,
//                guestEmail,
//                guestId
//        );
//
//        String jwt = JwtUtil.generateToken(
//                guestUser.getEmail(),
//                guestUser.getRole(),
//                guestUser.getProvider()
//        );
//
//        Cookie cookie = new Cookie("jwt", jwt);
//        cookie.setHttpOnly(true);
////        cookie.setSecure(true);
//        cookie.setSecure(false); // I'll change this later to true once in production
//        cookie.setPath("/");
//        cookie.setMaxAge(7 * 24 * 60 * 60);
//
//        response.addCookie(cookie);
//
//        return ResponseEntity.ok(Map.of(
//                "message", "Guest login successful",
//                "userId", guestUser.getId(),
//                "name", guestUser.getName(),
//                "email", guestUser.getEmail(),
//                "role", guestUser.getRole(),
//                "provider", guestUser.getProvider()
//        ));
//    }
//
//
//
//}

package com.nodetocode.nodetocode_backend.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.nodetocode.nodetocode_backend.model.User;
import com.nodetocode.nodetocode_backend.security.GoogleVerifier;
import com.nodetocode.nodetocode_backend.security.JwtUtil;
import com.nodetocode.nodetocode_backend.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private GoogleVerifier googleVerifier;

    @PostMapping("/oauth/google")
    public ResponseEntity<?> googleLogin(
            @RequestBody Map<String, String> body,
            HttpServletResponse response
    ) {
        try {

            String token = body.get("token");

            System.out.println("TOKEN: " + token);

            GoogleIdToken.Payload payload = googleVerifier.verifyToken(token);

            if (payload == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid Google token"));
            }

            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String providerId = payload.getSubject();

            User user = userService.processGoogleLogin(name, email, providerId);

            String jwt = JwtUtil.generateToken(
                    user.getEmail(),
                    user.getRole(),
                    user.getProvider()
            );

            setJwtCookie(response, jwt);

            return ResponseEntity.ok(Map.of(
                    "userId", user.getId(),
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "provider", user.getProvider()
            ));

        } catch (Exception e) {
            e.printStackTrace();  // 👈 add this for debugging
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Google authentication failed"));
        }
    }

    @PostMapping("/guest")
    public ResponseEntity<?> guestLogin(HttpServletResponse response) {

        String guestId = UUID.randomUUID().toString();
        String guestEmail = "guest_" + guestId + "@nodetocode.app";
        String guestName = "Guest User";

        User guestUser = userService.createGuestUser(
                guestName,
                guestEmail,
                guestId
        );

        String jwt = JwtUtil.generateToken(
                guestUser.getEmail(),
                guestUser.getRole(),
                guestUser.getProvider()
        );

        setJwtCookie(response, jwt);

        return ResponseEntity.ok(Map.of(
                "userId", guestUser.getId(),
                "name", guestUser.getName(),
                "email", guestUser.getEmail(),
                "role", guestUser.getRole(),
                "provider", guestUser.getProvider()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {

        String jwt = extractJwtFromCookie(request);

        if (jwt == null || !JwtUtil.validateToken(jwt)) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not authenticated"));
        }

        String email = JwtUtil.extractEmail(jwt);
        User user = userService.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "User not found"));
        }

        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "provider", user.getProvider()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(false) // ⚠️ TRUE in production (HTTPS)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ── AI API Key Settings ──

    @GetMapping("/settings/api-key")
    public ResponseEntity<?> getApiKey() {
        try {
            User user = userService.getCurrentUser();
            String apiKey = userService.getApiKey(user);
            boolean hasKey = apiKey != null && !apiKey.isBlank();
            // Return masked key for security (only show last 4 chars)
            String maskedKey = hasKey
                    ? "••••••••" + apiKey.substring(Math.max(0, apiKey.length() - 4))
                    : "";
            return ResponseEntity.ok(Map.of(
                    "hasApiKey", hasKey,
                    "maskedKey", maskedKey
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Not authenticated"));
        }
    }

    @PutMapping("/settings/api-key")
    public ResponseEntity<?> updateApiKey(@RequestBody Map<String, String> body) {
        try {
            User user = userService.getCurrentUser();
            String apiKey = body.get("apiKey");
            userService.saveApiKey(user, apiKey);
            return ResponseEntity.ok(Map.of("message", "API key updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(Map.of("error", "Failed to update API key"));
        }
    }

    @DeleteMapping("/settings/api-key")
    public ResponseEntity<?> removeApiKey() {
        try {
            User user = userService.getCurrentUser();
            userService.saveApiKey(user, null);
            return ResponseEntity.ok(Map.of("message", "API key removed"));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(Map.of("error", "Failed to remove API key"));
        }
    }

    private void setJwtCookie(HttpServletResponse response, String jwt) {

        ResponseCookie cookie = ResponseCookie.from("jwt", jwt)
                .httpOnly(true)
                .secure(false) // ⚠️ TRUE in production
                .path("/")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String extractJwtFromCookie(HttpServletRequest request) {

        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("jwt".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
