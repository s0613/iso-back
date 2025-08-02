package com.isoplatform.api.certification;

import com.isoplatform.api.certification.repository.CertificateRepository;
import com.isoplatform.api.certification.request.CertificateRequest;
import com.isoplatform.api.certification.response.CertificateResponse;
import com.isoplatform.api.certification.service.CertificateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CertificateServiceTest {

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private CertificateRepository certificateRepository;

    @Test
    public void testIssueCertificate() {
        // Given
        CertificateRequest request = new CertificateRequest();
        request.setVin("KMHD35LE5HU123456");
        request.setManufacturer("현대자동차");
        request.setModelName("아반떼");
        request.setManufactureYear(2020);
        request.setFirstRegisterDate(LocalDate.of(2020, 3, 15));
        request.setMileage(50000);
        request.setInspectDate(LocalDate.now());
        request.setInspectorCode("INS001");
        request.setInspectorName("김검사");

        // When
        CertificateResponse response = certificateService.issueCertificate(request, "admin");

        // Then
        assertNotNull(response);
        assertNotNull(response.getCertNumber());
        assertEquals(request.getVin(), response.getVin());
        assertEquals(request.getManufacturer(), response.getManufacturer());
        assertEquals(request.getModelName(), response.getModelName());
        assertEquals("admin", response.getIssuedBy());

        // 데이터베이스에 저장되었는지 확인
        assertTrue(certificateRepository.existsByVin(request.getVin()));
    }

    @Test
    public void testDuplicateVinThrowsException() {
        // Given
        CertificateRequest request = new CertificateRequest();
        request.setVin("KMHD35LE5HU123456");
        request.setManufacturer("현대자동차");
        request.setModelName("아반떼");
        request.setInspectDate(LocalDate.now());
        request.setInspectorCode("INS001");
        request.setInspectorName("김검사");

        // 첫 번째 인증서 발급
        certificateService.issueCertificate(request, "admin");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            certificateService.issueCertificate(request, "admin");
        });
    }
}
