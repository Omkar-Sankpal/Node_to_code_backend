//package com.nodetocode.nodetocode_backend.security;
//
//import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
//import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
//import com.google.api.client.http.javanet.NetHttpTransport;
//import com.google.api.client.json.jackson2.JacksonFactory;
//import org.springframework.beans.factory.annotation.Value;
//
//import java.util.Collections;
//
//public class GoogleVerifier {
//
//    @Value("${google.client.id}")
//    private static String CLIENT_ID;
//
//    public static GoogleIdToken.Payload verifyToken(String token) throws Exception {
//
//        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
//                new NetHttpTransport(),
//                JacksonFactory.getDefaultInstance()
//        )
//                .setAudience(Collections.singletonList(CLIENT_ID))
//                .build();
//
//        GoogleIdToken idToken = verifier.verify(token);
//
//        System.out.println("ID TOKEN OBJECT: " + idToken);
//
//        if (idToken != null) {
//            return idToken.getPayload();
//        } else {
//            throw new Exception("Invalid ID token.");
//        }
//    }
//}

package com.nodetocode.nodetocode_backend.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class GoogleVerifier {

    @Value("${google.client.id}")
    private String clientId;

    public GoogleIdToken.Payload verifyToken(String token) throws Exception {

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken idToken = verifier.verify(token);

        System.out.println("CLIENT ID USED: " + clientId);
        System.out.println("ID TOKEN OBJECT: " + idToken);

        if (idToken != null) {
            return idToken.getPayload();
        } else {
            throw new Exception("Invalid ID token.");
        }
    }
}