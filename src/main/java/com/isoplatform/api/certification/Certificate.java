package com.isoplatform.api.certification;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // PK

    // 인증서 고유 정보
    @Column(nullable = false, unique = true)
    private String certNumber;     // 인증번호

    @Column(nullable = false)
    private LocalDate issueDate;   // 발급일자

    @Column(nullable = false)
    private LocalDate expireDate;  // 유효일자

    @Column(nullable = false)
    private LocalDate inspectDate; // 검사일자

    // 자동차 정보
    @Column(nullable = false)
    private String manufacturer;   // 제조사

    @Column(nullable = false)
    private String modelName;      // 모델명

    @Column(nullable = false, unique = true)
    private String vin;            // 차대번호

    private Integer manufactureYear;       // 제조연도
    private LocalDate firstRegisterDate;   // 최초등록일
    private Integer mileage;               // 주행거리

    // 평가사 정보
    private String inspectorCode;  // 평가사 코드
    private String inspectorName;  // 평가사 성명

    // PDF 관련 필드 추가
    private String pdfS3Key;          // S3 키
    private String pdfUrl;            // CloudFront URL
    // 서명 이미지 (선택)
    private String signaturePath;  // Authorized Signature 이미지 경로 (S3 또는 서버 경로)

    // 발급 메타데이터
    private String issuedBy;       // 발급자(관리자 아이디 등)
}

