package com.stemlink.skillmentor.configs;

import com.stemlink.skillmentor.security.ClerkValidator;
import com.stemlink.skillmentor.security.SkillMentorJwtValidator;
import com.stemlink.skillmentor.security.TokenValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ValidatorConfiguration {

    /**
     * Used when auth.validator.type=stem-link in application.properties
     * Validates tokens signed with an internal HMAC secret key.
     */
    @Bean
    @ConditionalOnProperty(name = "auth.validator.type", havingValue = "stem-link")
    public TokenValidator jwtTokenValidator(
            @Value("${jwt.secret:my-secret-key-must-be-at-least-32-characters-long-for-HS256}") String jwtSecret
    ) {
        log.info("SkillMentor JWT validator configured as primary TokenValidator");
        return new SkillMentorJwtValidator(jwtSecret);
    }

    /**
     * Used when auth.validator.type=clerk (or when the property is missing - default).
     *
     * IMPORTANT - clerk.jwks.url must be the BASE DOMAIN only, e.g.:
     *   clerk.jwks.url=https://brief-doe-4.clerk.accounts.dev
     *
     * Do NOT include /.well-known/jwks or /.well-known/jwks.json in the URL.
     * JwkProviderBuilder appends that path automatically. If you include it,
     * the library double-appends it and key lookup will silently fail with a 404,
     * causing every token to be rejected with 401 Unauthorized.
     */
    @Bean
    @ConditionalOnProperty(name = "auth.validator.type", havingValue = "clerk", matchIfMissing = true)
    public TokenValidator clerkTokenValidator(
            @Value("${clerk.jwks.url}") String clerkJwksUrl
    ) {
        log.info("Clerk validator configured as primary TokenValidator with URL: {}", clerkJwksUrl);
        return new ClerkValidator(clerkJwksUrl);
    }
}