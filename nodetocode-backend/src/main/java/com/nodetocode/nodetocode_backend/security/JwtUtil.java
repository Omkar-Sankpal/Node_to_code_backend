// JwtUtil.java
package com.nodetocode.nodetocode_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtil {

    private static final String SECRET = "MY_SUPER_SECRET_KEY_MY_SUPER_SECRET_KEY";
    private static final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    public static String generateToken(String email, String role, String provider) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("provider", provider)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Jws<Claims> parseClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}
