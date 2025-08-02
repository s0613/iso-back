package com.isoplatform.api.config.filter;

import com.isoplatform.api.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 쿠키/헤더 검증 필터
 */
@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final String secretKey;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        // 인증이 필요없는 경로는 필터 건너뛰기
        // 변경 후
        if (requestUri.startsWith("/api/auth/signup") ||
                requestUri.startsWith("/api/auth/login") ||
                requestUri.startsWith("/api/health") ||
                requestUri.startsWith("/swagger-ui") ||
                requestUri.startsWith("/v3/api-docs")) {

            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1) 쿠키에서 JWT 추출
            String token = getTokenFromCookie(request);

            // 2) 헤더에서 JWT 추출 (쿠키에 없거나 null일 경우)
            if (token == null) {
                final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    token = authorization.substring(7); // "Bearer " 이후
                }
            }

            // 3) 토큰이 없다면 그냥 진행
            if (token == null || token.isBlank()) {
                log.debug("[JwtFilter] JWT가 쿠키나 헤더에 없습니다. URI={}", requestUri);
                filterChain.doFilter(request, response);
                return;
            }

            // 4) 토큰 만료 여부 확인
            if (JwtUtil.isExpired(token, secretKey)) {
                log.debug("[JwtFilter] JWT가 만료되었습니다. token={}", token);
                filterChain.doFilter(request, response);
                return;
            }

            // 5) 토큰에서 email 추출
            String email = JwtUtil.getEmail(token, secretKey);


            // 이메일이 null이 아니고 아직 인증되지 않은 상태라면
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    // DB/서비스에서 사용자 조회
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    // 인증 토큰 생성 후 SecurityContext 에 등록
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                } catch (Exception e) {
                    // 사용자 로드 중 예외 발생시 로그만 남기고 계속 진행
                    log.error("[JwtFilter] 사용자 정보 로드 중 오류: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            // JWT 처리 중 어떤 예외가 발생하더라도 오류를 발생시키지 않고 로그만 남김
            log.error("[JwtFilter] JWT 처리 중 예외 발생: {}", e.getMessage());
        }

        // 어떤 경우든 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 쿠키에 "auth-token" 항목이 있는지 확인하여 반환
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie c : cookies) {
            if ("auth-token".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}