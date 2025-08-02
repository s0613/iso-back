package com.isoplatform.api.certification.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CertificateRequest {

    @NotBlank(message = "차대번호는 필수입니다")
    private String vin;            // 차대번호

    @NotBlank(message = "제조사는 필수입니다")
    private String manufacturer;   // 제조사

    @NotBlank(message = "모델명은 필수입니다")
    private String modelName;      // 모델명

    private Integer manufactureYear;       // 제조연도
    private LocalDate firstRegisterDate;   // 최초등록일
    private Integer mileage;               // 주행거리

    @NotNull(message = "검사일자는 필수입니다")
    private LocalDate inspectDate; // 검사일자

    @NotBlank(message = "평가사 코드는 필수입니다")
    private String inspectorCode;  // 평가사 코드

    @NotBlank(message = "평가사 성명은 필수입니다")
    private String inspectorName;  // 평가사 성명

    private String signaturePath;  // 서명 이미지 경로 (선택)

    // PDF에 추가로 들어갈 수 있는 필드들
    private String certNumber;     // 인증서 번호 (자동생성되지만 수동 지정 가능)
    private LocalDate issueDate;   // 발급일 (기본값: 현재날짜)
    private LocalDate expireDate;  // 만료일 (기본값: 1년 후)
    private String issuedBy;       // 발급기관명 (컨트롤러에서 전달되지만 요청에서도 받을 수 있음)
}
