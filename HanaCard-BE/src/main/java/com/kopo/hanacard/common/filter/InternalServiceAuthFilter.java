package com.kopo.hanacard.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {
    
    @Value("${internal.service.secret}")
    private String secret;

    @Value("${internal.auth.header}")
    private String authHeader;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        
        // 내부 API 요청인지 확인
        if (requestURI.startsWith("/api/integration/")) {
            String internalAuth = request.getHeader(authHeader);
            
            if (internalAuth == null || !validateInternalAuth(internalAuth)) {
                log.warn("내부 서비스 인증 실패: URI={}, Auth={}", requestURI, internalAuth);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Unauthorized internal service access\"}");
                return;
            }
            
            log.debug("내부 서비스 인증 성공: URI={}", requestURI);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean validateInternalAuth(String authHeader) {
        try {
            // Base64 디코딩하여 시크릿 확인
            String decoded = new String(Base64.getDecoder().decode(authHeader));
            return secret.equals(decoded);
        } catch (Exception e) {
            log.error("내부 서비스 인증 토큰 파싱 실패", e);
            return false;
        }
    }
}











