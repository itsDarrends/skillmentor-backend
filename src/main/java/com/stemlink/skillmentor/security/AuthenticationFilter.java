package com.stemlink.skillmentor.security;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private final TokenValidator tokenValidator;

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            log.debug("Authorization header found, attempting token validation");

            if (tokenValidator.validateToken(token)) {
                String userId    = tokenValidator.extractUserId(token);
                String email     = tokenValidator.extractEmail(token);
                String firstName = tokenValidator.extractFirstName(token);
                String lastName  = tokenValidator.extractLastName(token);

                UserPrincipal userPrincipal = new UserPrincipal(userId, email, firstName, lastName);

                List<String> roles = tokenValidator.extractRoles(token);
                List<GrantedAuthority> authorities = roles != null
                        ? roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList())
                        : new ArrayList<>();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userPrincipal, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authentication set for user: {}", userId);
            } else {
                log.warn("Token validation failed for request: {}", request.getRequestURI());
            }
        } else {
            log.debug("No Authorization header found for request: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}