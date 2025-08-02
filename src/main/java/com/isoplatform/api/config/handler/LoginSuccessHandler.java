package com.isoplatform.api.config.handler;

import com.isoplatform.api.config.UserPrincipal;
import com.isoplatform.api.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    final private ObjectMapper objectMapper;
    final private String secretKey;
    final private long expirationTime;
    final private Environment environment;

    public LoginSuccessHandler(ObjectMapper objectMapper, String secretKey, long expirationTime, Environment environment) {
        this.objectMapper = objectMapper;
        this.secretKey = secretKey;
        this.expirationTime = expirationTime;
        this.environment = environment;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // JWT 토큰 생성
        String token = JwtUtil.generateToken(
                principal.getUsername(),
                principal.getUser().getId(),
                secretKey,
                expirationTime
        );

        // 현재 프로필 확인 (local 또는 dev)
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isLocal = Arrays.asList(activeProfiles).contains("local");

        // JWT 토큰을 쿠키에 저장
        Cookie jwtCookie = new Cookie("auth-token", token);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true);
        jwtCookie.setMaxAge((int) (expirationTime / 1000)); // 밀리초를 초로 변환

        // 환경에 따른 쿠키 설정
        if (isLocal) {
            // 로컬 환경 설정
            jwtCookie.setSecure(false);
            jwtCookie.setAttribute("SameSite", "Lax");
        } else {
            // 개발 환경 설정
            jwtCookie.setSecure(true);
            jwtCookie.setAttribute("SameSite", "None");
            // 도메인 설정 (개발 서버 도메인으로 변경 필요)
            jwtCookie.setDomain("dev.esgdashboard.com");
        }

        response.addCookie(jwtCookie);

        // 응답 데이터 생성 (쿠키와 별도로 응답 본문에도 정보 제공)
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("userId", principal.getUser().getId());
        responseData.put("email", principal.getUsername());
        responseData.put("role", principal.getUser().getRole());

        // 응답 설정 및 반환
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(UTF_8.name());
        response.setStatus(SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(responseData));
    }
}