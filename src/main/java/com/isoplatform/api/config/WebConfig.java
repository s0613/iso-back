package com.isoplatform.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/files/open/**")
                .allowedOriginPatterns("*")   // 또는 구체적인 도메인 배열
                .allowedMethods("GET")
                .allowedHeaders("*")
                .allowCredentials(false);

        registry.addMapping("/**")
                // 허용할 오리진을 로컬+프로덕션 도메인으로 늘립니다
                .allowedOrigins(
                        "http://localhost:3000"
                )
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")      // (선택) 모든 헤더 허용
                .exposedHeaders("Set-Cookie"); // (선택) 클라이언트에서 쿠키 헤더 확인 용도
    }
}

