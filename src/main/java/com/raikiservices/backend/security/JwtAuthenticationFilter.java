package com.raikiservices.backend.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authentifie la requête à partir de l'en-tête {@code Authorization: Bearer <access token>}.
 *
 * <p>En l'absence d'en-tête ou si le jeton est invalide, le filtre laisse passer sans
 * authentifier : c'est {@link JwtAuthenticationEntryPoint} qui décidera de refuser ou non,
 * selon que la route visée exige une authentification.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";
    private static final String SSE_STREAM_PATH = "/api/v1/notifications/stream";
    private static final String STREAM_TOKEN_PARAM = "access_token";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String email = jwtService.extractSubject(token, JwtService.TYPE_ACCESS);
            if (email != null) {
                authenticate(request, email);
            }
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String email) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!userDetails.isEnabled()) {
                return;
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UsernameNotFoundException e) {
            // Jeton valide mais compte supprimé entre-temps : on reste non authentifié.
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length()).trim();
        }
        return resolveStreamToken(request);
    }

    /**
     * Jeton passé en paramètre d'URL, accepté uniquement sur le flux SSE.
     *
     * <p>L'API {@code EventSource} des navigateurs ne permet pas d'ajouter d'en-tête
     * {@code Authorization} : sans cette exception, le flux temps réel serait inaccessible.
     *
     * <p>Un jeton dans une URL peut se retrouver dans les journaux d'un proxy ; l'exception est
     * donc strictement limitée à ce chemin, et l'access token n'est valide que quelques minutes.
     */
    private String resolveStreamToken(HttpServletRequest request) {
        if (!SSE_STREAM_PATH.equals(request.getRequestURI())) {
            return null;
        }
        String token = request.getParameter(STREAM_TOKEN_PARAM);
        return (token == null || token.isBlank()) ? null : token.trim();
    }
}
