package com.stemlink.skillmentor.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.net.URL;
import java.security.PublicKey;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Slf4j
public class ClerkValidator implements TokenValidator {

    private final JwkProvider jwkProvider;

    public ClerkValidator(@Value("${clerk.jwks.url}") String clerkJwksUrl) {
        try {
            // JwkProviderBuilder with caching to avoid hitting Clerk on every request
            // Pass just the base domain - the library handles /.well-known/jwks.json automatically
            this.jwkProvider = new JwkProviderBuilder(new URL(clerkJwksUrl))
                    .cached(10, 24, TimeUnit.HOURS)
                    .rateLimited(10, 1, TimeUnit.MINUTES)
                    .build();
            log.info("ClerkValidator initialized successfully with JWKS URL: {}", clerkJwksUrl);
        } catch (Exception e) {
            log.error("Failed to initialize JwkProvider with URL: {}", clerkJwksUrl, e);
            throw new RuntimeException("Failed to initialize Clerk validator", e);
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            if (decodedJWT == null) {
                log.error("Failed to decode token");
                return false;
            }

            String kid = decodedJWT.getKeyId();
            if (kid == null || kid.isEmpty()) {
                log.error("Token does not contain a key ID (kid)");
                return false;
            }

            log.debug("Token kid: {}", kid);

            if (!verifyTokenSignature(token, kid)) {
                log.error("Token signature verification failed");
                return false;
            }

            log.debug("Token validation successful for subject: {}", decodedJWT.getSubject());
            return true;

        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String extractUserId(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getSubject() : null;
        } catch (Exception e) {
            log.error("Error extracting user ID: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<String> extractRoles(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            if (decodedJWT == null) {
                return null;
            }
            return decodedJWT.getClaim("roles").asList(String.class);
        } catch (Exception e) {
            log.error("Error extracting roles: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String extractFirstName(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getClaim("first_name").asString() : null;
        } catch (Exception e) {
            log.error("Error extracting first name: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String extractLastName(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getClaim("last_name").asString() : null;
        } catch (Exception e) {
            log.error("Error extracting last name: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String extractEmail(String token) {
        try {
            DecodedJWT decodedJWT = decodeToken(token);
            return decodedJWT != null ? decodedJWT.getClaim("email").asString() : null;
        } catch (Exception e) {
            log.error("Error extracting email: {}", e.getMessage());
            return null;
        }
    }

    private DecodedJWT decodeToken(String token) {
        try {
            return JWT.decode(token);
        } catch (Exception e) {
            log.error("Failed to decode token: {}", e.getMessage());
            return null;
        }
    }

    private boolean verifyTokenSignature(String token, String kid) {
        try {
            Jwk jwk = jwkProvider.get(kid);
            PublicKey publicKey = jwk.getPublicKey();
            Algorithm algorithm = Algorithm.RSA256((java.security.interfaces.RSAPublicKey) publicKey, null);
            JWT.require(algorithm).build().verify(token);
            log.debug("Token signature verified successfully for kid: {}", kid);
            return true;
        } catch (Exception e) {
            log.error("Signature verification failed for kid {}: {}", kid, e.getMessage(), e);
            return false;
        }
    }
}