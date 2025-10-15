package com.kopo.hanacard.common.filter;

import com.kopo.hanacard.user.domain.User;
import com.kopo.hanacard.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerInfoAuthFilter extends OncePerRequestFilter {
    
    private final UserRepository userRepository;
    
    private static final String CUSTOMER_INFO_TOKEN_HEADER = "X-Customer-Info-Token";
    private static final String UNIFIED_AUTH_TOKEN_HEADER = "X-Unified-Auth-Token";
    private static final String REQUESTING_SERVICE_HEADER = "X-Requesting-Service";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        
        // 카드 관련 API 요청인지 확인
        if (requestURI.startsWith("/cards/")) {
            String unifiedAuthToken = request.getHeader(UNIFIED_AUTH_TOKEN_HEADER);
            String customerInfoToken = request.getHeader(CUSTOMER_INFO_TOKEN_HEADER);
            String requestingService = request.getHeader(REQUESTING_SERVICE_HEADER);
            
            // 통합 토큰 우선 처리
            if (unifiedAuthToken != null) {
                try {
                    User user = extractUserFromUnifiedToken(unifiedAuthToken);
                    
                    if (user == null) {
                        log.warn("통합 인증 토큰에서 사용자 정보를 찾을 수 없음 - 토큰: {}", unifiedAuthToken);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("{\"error\":\"Invalid unified auth token\"}");
                        return;
                    }
                    
                    // 사용자 ID를 요청 속성에 저장
                    request.setAttribute("userId", user.getId());
                    request.setAttribute("user", user);
                    
                    log.info("통합 인증 성공: URI={}, UserId={}, 전화번호={}", requestURI, user.getId(), user.getPhoneNumber());
                    
                } catch (Exception e) {
                    log.error("통합 인증 토큰 처리 중 오류 발생", e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("{\"error\":\"Unified token processing error\"}");
                    return;
                }
            }
            // 기존 CI 토큰 처리 (하위 호환성)
            else if (customerInfoToken != null) {
                if (requestingService == null) {
                    log.warn("요청 서비스 헤더 누락: URI={}", requestURI);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Missing requesting service header\"}");
                return;
            }
            
            // 요청 서비스 검증
            if (!"GREEN_WORLD".equals(requestingService)) {
                log.warn("허용되지 않은 요청 서비스: {}", requestingService);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\":\"Unauthorized requesting service\"}");
                return;
            }
            
            try {
                // CI 토큰에서 사용자 정보 추출
                User user = extractUserFromCustomerToken(customerInfoToken);
                
                if (user == null) {
                        log.warn("고객 정보 토큰에서 사용자 정보를 찾을 수 없음 - 토큰: {}", customerInfoToken);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Invalid customer information token\"}");
                    return;
                }
                
                // 사용자 ID를 요청 속성에 저장
                request.setAttribute("userId", user.getId());
                request.setAttribute("user", user);
                
                    log.info("고객 정보 인증 성공: URI={}, UserId={}, 전화번호={}", requestURI, user.getId(), user.getPhoneNumber());
                
            } catch (Exception e) {
                log.error("고객 정보 토큰 처리 중 오류 발생", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"error\":\"Token processing error\"}");
                    return;
                }
            } else {
                log.warn("인증 토큰 누락: URI={}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Missing authentication token\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private User extractUserFromCustomerToken(String customerInfoToken) {
        try {
            // Base64 디코딩하여 CI 추출
            String ci = new String(Base64.getDecoder().decode(customerInfoToken));
            log.debug("추출된 CI: {}", maskCi(ci));
            
            // CI로 직접 사용자 조회
            Optional<User> userOpt = userRepository.findByCi(ci);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                log.info("CI 기반 사용자 조회 성공: ID={}, CI={}", user.getId(), maskCi(ci));
                return user;
            } else {
                log.warn("CI에 해당하는 사용자를 찾을 수 없음: CI={}", maskCi(ci));
                return null;
            }
            
        } catch (Exception e) {
            log.error("고객 정보 토큰에서 사용자 정보 추출 실패", e);
            return null;
        }
    }

    private User extractUserFromUnifiedToken(String unifiedAuthToken) {
        try {
            // Base64 디코딩
            String decoded = new String(Base64.getDecoder().decode(unifiedAuthToken));
            log.debug("디코딩된 통합 토큰: {}", decoded);
            
            // 통합 토큰 형식 파싱 (CI_전화번호_해시_UNIFIED 형식)
            if (decoded.endsWith("_UNIFIED")) {
                // 새로운 형식: CI_전화번호_해시_UNIFIED
                String ci = decoded.replace("_UNIFIED", "");
                log.debug("통합 토큰 CI 추출: {}", ci);
                
                // CI에서 전화번호 추출
                String phoneNumber = extractPhoneFromCI(ci);
                
                // 전화번호로 사용자 조회
                Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    log.info("통합 토큰으로 사용자 조회 성공: ID={}, 전화번호={}", user.getId(), phoneNumber);
                    return user;
                } else {
                    log.warn("통합 토큰에 해당하는 사용자를 찾을 수 없음: {}", phoneNumber);
                    return null;
                }
            } else {
                // 기존 형식: CI|타임스탬프|서명
                String[] parts = decoded.split("\\|");
                if (parts.length != 3) {
                    log.warn("잘못된 통합 토큰 형식: {}", decoded);
                    return null;
                }
                
                String ci = parts[0];
                long timestamp = Long.parseLong(parts[1]);
                String signature = parts[2];
                
                // 토큰 유효 시간 체크 (1시간)
                long currentTime = System.currentTimeMillis();
                if (currentTime - timestamp > 3600000) {
                    log.warn("통합 토큰 만료: {}", decoded);
                    return null;
                }
                
                // CI에서 전화번호 추출
                String phoneNumber = extractPhoneFromCI(ci);
                
                // 전화번호로 사용자 조회
                Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    log.info("통합 토큰으로 사용자 조회 성공: ID={}, 전화번호={}", user.getId(), phoneNumber);
                    return user;
                } else {
                    log.warn("통합 토큰에 해당하는 사용자를 찾을 수 없음: {}", phoneNumber);
                    return null;
                }
            }
            
        } catch (Exception e) {
            log.error("통합 인증 토큰에서 사용자 정보 추출 실패", e);
            return null;
        }
    }

    private String extractPhoneFromCI(String ci) {
        // CI_01012345678_123456 형식에서 전화번호 추출
        if (ci.startsWith("CI_") && ci.contains("_")) {
            String[] parts = ci.split("_");
            if (parts.length >= 2) {
                String phoneDigits = parts[1];
                // 01012345678 -> 010-1234-5678 형식으로 변환
                if (phoneDigits.length() == 11 && phoneDigits.startsWith("010")) {
                    return phoneDigits.substring(0, 3) + "-" + 
                           phoneDigits.substring(3, 7) + "-" + 
                           phoneDigits.substring(7);
                }
            }
        }
        
        // 기본값 반환
        log.warn("CI에서 올바른 전화번호를 찾을 수 없음, 기본값 사용: {}", ci);
        return "010-1234-5678";
    }

    private String extractPhoneFromToken(String decodedToken) {
        // CI:010-1234-5678 형식에서 전화번호 추출
        if (decodedToken.startsWith("CI:")) {
            return decodedToken.substring(3);
        }
        
        // 직접 전화번호인 경우
        if (decodedToken.matches("010-\\d{4}-\\d{4}")) {
            return decodedToken;
        }
        
        // 기본값 반환
        log.warn("토큰에서 올바른 전화번호를 찾을 수 없음, 기본값 사용: {}", decodedToken);
        return "010-1234-5678";
    }

    private String maskCi(String ci) {
        if (ci == null || ci.length() < 8) {
            return "****";
        }
        return ci.substring(0, 4) + "****" + ci.substring(ci.length() - 4);
    }
}
