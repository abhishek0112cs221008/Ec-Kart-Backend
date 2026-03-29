package com.ecommerce.ecommercebackend.auth.jwt;

import com.ecommerce.ecommercebackend.auth.service.JWTservice;
import com.ecommerce.ecommercebackend.auth.service.UsersServices;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JWTFilter extends OncePerRequestFilter {

    private final JWTservice jwtservice;
    private final UsersServices userService;
    private final AuthenticationEntryPoint restAuthenticationEntryPoint; // inject your RestAuthenticationEntryPoint bean

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                username = jwtservice.extractUsername(token); // can throw ExpiredJwtException or other JwtException
            }

            if (username != null && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.loadUserByUsername(username);

                if (jwtservice.validateToken(token, userDetails)) {
                    var claims = jwtservice.extractRoles(token);
                    var authorities = claims.stream()
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                            .toList();

                    var authToken = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);

        } catch (JwtException e) {
            System.err.println("JWT Authentication Failed: " + e.getMessage());
            InsufficientAuthenticationException authEx = new InsufficientAuthenticationException("Invalid or expired JWT: " + e.getMessage());
            restAuthenticationEntryPoint.commence(request, response, authEx);
        } catch (Exception e) {
            System.err.println("Critical Authentication Filter Error: " + e.getMessage());
            InsufficientAuthenticationException authEx = new InsufficientAuthenticationException("Authentication failed: " + e.getMessage());
            restAuthenticationEntryPoint.commence(request, response, authEx);
        }
    }
}

