package com.example.event_board.security;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;

public class GenerateKeys {
    public static void main(String[] args) {

        SecretKey accessKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String base64AccessKey = Base64.getEncoder().encodeToString(accessKey.getEncoded());

        SecretKey refreshKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String base64RefreshKey = Base64.getEncoder().encodeToString(refreshKey.getEncoded());

        System.out.println("Access Key: " + base64AccessKey);
        System.out.println("Refresh Key: " + base64RefreshKey);
    }
}
