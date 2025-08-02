package com.isoplatform.api.certification.controller;

import com.isoplatform.api.certification.request.CertificateRequest;
import com.isoplatform.api.certification.response.CertificateResponse;
import com.isoplatform.api.certification.service.CertificateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    /**
     * 인증서 발급
     */
    @PostMapping("/issue")
    public ResponseEntity<CertificateResponse> issueCertificate(
            @Valid @RequestBody CertificateRequest request,
            Authentication authentication) {

        log.info("인증서 발급 요청 - VIN: {}, 발급자: {}", request.getVin(), authentication.getName());

        CertificateResponse response = certificateService.issueCertificate(request, authentication.getName());

        log.info("인증서 발급 완료 - 인증번호: {}", response.getCertNumber());

        return ResponseEntity.ok(response);
    }


    /**
     * 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("잘못된 요청: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
        log.error("서버 오류", e);
        return ResponseEntity.internalServerError().body("서버 내부 오류가 발생했습니다.");
    }
}
