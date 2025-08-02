package com.isoplatform.api.certification.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponse {
    private Long id;
    private String certNumber;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private LocalDate inspectDate;
    private String manufacturer;
    private String modelName;
    private String vin;
    private Integer manufactureYear;
    private LocalDate firstRegisterDate;
    private Integer mileage;
    private String inspectorCode;
    private String inspectorName;
    private String issuedBy;
    private String pdfFilePath;  // CloudFront URL
}
