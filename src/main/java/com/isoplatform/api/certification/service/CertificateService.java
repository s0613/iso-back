package com.isoplatform.api.certification.service;

import com.isoplatform.api.certification.Certificate;
import com.isoplatform.api.certification.repository.CertificateRepository;
import com.isoplatform.api.certification.request.CertificateRequest;
import com.isoplatform.api.certification.response.CertificateResponse;
import com.isoplatform.api.util.S3Service;
import com.isoplatform.api.util.S3UploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.PDResources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final S3Service               s3Service;
    private final CertificateRepository   certificateRepository;

    private static final String TEMPLATE_PDF = "static/ISO_acrobat.pdf";
    private static final String FONT_PATH    = "static/fonts/Pretendard-Medium.ttf";
    private static final String OUTPUT_DIR   = "certificates/";

    @Transactional
    public CertificateResponse issueCertificate(CertificateRequest req, String issuedBy) {
        try {
            Certificate dup = certificateRepository.findByVin(req.getVin()).orElse(null);
            if (dup != null) return toResponse(dup);

            Certificate cert   = toEntity(req, issuedBy);
            String      localPdf = createPdf(cert);

            String        s3Key = "certificates/" + cert.getCertNumber() + ".pdf";
            S3UploadResult up   = s3Service.uploadFile(localPdf, s3Key);
            cert.setPdfS3Key(up.getS3Key());
            cert.setPdfUrl(up.getCloudFrontUrl());
            certificateRepository.save(cert);
            s3Service.deleteLocalFile(localPdf);

            return toResponse(cert);

        } catch (Exception e) {
            log.error("issueCertificate error", e);
            throw new RuntimeException("인증서 발급 실패: " + e.getMessage());
        }
    }

    private String createPdf(Certificate c) throws Exception {
        Path dir = Paths.get(OUTPUT_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        String outFile = OUTPUT_DIR + c.getCertNumber() + ".pdf";

        try (InputStream tmpl = new ClassPathResource(TEMPLATE_PDF).getInputStream();
             PDDocument doc   = PDDocument.load(tmpl)) {

            PDAcroForm form = Objects.requireNonNull(doc.getDocumentCatalog().getAcroForm());

            // ── (추가) 폼 필드 이름 모두 찍기 ──
            log.info(">> PDF 폼 필드 목록 시작");
            for (PDField f : form.getFields()) {
                log.info("   • field ▶ {}", f.getFullyQualifiedName());
            }
            log.info(">> PDF 폼 필드 목록 끝");

            // 2-1. Pretendard 글꼴 임베드 & 기본 글꼴 지정
            embedKoreanFont(doc, form);

            // 2-2. 필드 값 채우기
            fillFields(form, c);

            // 2-3. appearance 재생성 후 평면화
            form.refreshAppearances();
            form.flatten();

            doc.save(outFile);
            log.info("PDF 저장 완료: {}", outFile);
        }

        return outFile;
    }

    private void embedKoreanFont(PDDocument doc, PDAcroForm form) throws Exception {
        PDResources dr = form.getDefaultResources();
        if (dr == null) {
            dr = new PDResources();
            form.setDefaultResources(dr);
        }

        try (InputStream fontStream = new ClassPathResource(FONT_PATH).getInputStream()) {
            PDType0Font font = PDType0Font.load(doc, fontStream, false);
            String fontName = dr.add(font).getName();
            form.setDefaultAppearance("/" + fontName + " 12 Tf 0 g");

            for (PDField field : form.getFields()) {
                setFieldFont(field, font, fontName);
            }
        }
    }

    private void setFieldFont(PDField field, PDType0Font font, String fontName) {
        try {
            if (field instanceof org.apache.pdfbox.pdmodel.interactive.form.PDTextField textField) {
                String da = "/" + fontName + " 12 Tf 0 g";
                textField.setDefaultAppearance(da);

                for (PDAnnotationWidget widget : textField.getWidgets()) {
                    try {
                        if (widget.getNormalAppearanceStream() != null) {
                            PDResources widgetResources = widget.getNormalAppearanceStream().getResources();
                            if (widgetResources == null) {
                                widgetResources = new PDResources();
                                widget.getNormalAppearanceStream().setResources(widgetResources);
                            }
                            widgetResources.add(font);
                        }
                    } catch (Exception e) {
                        log.debug("위젯 리소스 설정 실패 (무시됨): {}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("필드 폰트 설정 실패: {}", field.getFullyQualifiedName(), e);
        }
    }

    private void fillFields(PDAcroForm form, Certificate c) {
        Map<String, String> map = Map.ofEntries(
                Map.entry("certNumber",                  c.getCertNumber()),
                Map.entry("issueDate_es_:date",          date(c.getIssueDate())),
                Map.entry("expireDate_es_:date",         date(c.getExpireDate())),
                Map.entry("inspectDate_es_:date",        date(c.getInspectDate())),
                Map.entry("manu_es_:fullname",           c.getManufacturer()),
                Map.entry("modelName",                   c.getModelName()),
                Map.entry("vin",                         c.getVin()),
                Map.entry("manufactureYear_es_:date",    num(c.getManufactureYear())),
                Map.entry("firstRegisterDate_es_:date",  date(c.getFirstRegisterDate())),
                Map.entry("mileage",                     num(c.getMileage()) + (c.getMileage() != null ? " km" : "")),
                Map.entry("inspectorCode",               c.getInspectorCode()),
                Map.entry("inspectorName_es_:fullname",  c.getInspectorName()),
                Map.entry("corpName_es_:fullname",       c.getIssuedBy())
        );

        map.forEach((name, value) -> {
            PDField f = form.getField(name);
            if (f == null) {
                log.warn("필드 없음: {}", name);
                return;
            }
            try {
                String safeValue = value == null ? "" : value;
                if (containsKorean(safeValue)) {
                    setKoreanFieldValue(f, safeValue);
                } else {
                    f.setValue(safeValue);
                }
            } catch (Exception e) {
                log.error("setValue {} error: {}", name, e.getMessage());
                try {
                    f.setValue("");
                } catch (IOException ex) {
                    log.error("빈 값 설정도 실패: {}", name, ex);
                }
            }
        });
    }

    private boolean containsKorean(String text) {
        if (text == null || text.isEmpty()) return false;
        return text.chars().anyMatch(c ->
                (c >= 0xAC00 && c <= 0xD7AF) ||
                        (c >= 0x1100 && c <= 0x11FF) ||
                        (c >= 0x3130 && c <= 0x318F)
        );
    }

    private void setKoreanFieldValue(PDField field, String value) throws IOException {
        if (field instanceof org.apache.pdfbox.pdmodel.interactive.form.PDTextField textField) {
            textField.setValue(value);
        } else {
            field.setValue(value);
        }
    }

    private Certificate toEntity(CertificateRequest r, String by){
        return certificateRepository.save(
                Certificate.builder()
                        .certNumber(r.getCertNumber() != null ? r.getCertNumber() : genCert())
                        .issueDate(r.getIssueDate() != null ? r.getIssueDate() : LocalDate.now())
                        .expireDate(r.getExpireDate() != null ? r.getExpireDate() :
                                   (r.getIssueDate() != null ? r.getIssueDate().plusYears(1) : LocalDate.now().plusYears(1)))
                        .inspectDate(r.getInspectDate())
                        .manufacturer(r.getManufacturer())
                        .modelName(r.getModelName())
                        .vin(r.getVin())
                        .manufactureYear(r.getManufactureYear())
                        .firstRegisterDate(r.getFirstRegisterDate())
                        .mileage(r.getMileage())
                        .inspectorCode(r.getInspectorCode())
                        .inspectorName(r.getInspectorName())
                        .signaturePath(r.getSignaturePath())
                        .issuedBy(r.getIssuedBy() != null ? r.getIssuedBy() : by)
                        .build()
        );
    }

    private CertificateResponse toResponse(Certificate c) {
        return CertificateResponse.builder()
                .id(c.getId())
                .certNumber(c.getCertNumber())
                .issueDate(c.getIssueDate())
                .expireDate(c.getExpireDate())
                .inspectDate(c.getInspectDate())
                .manufacturer(c.getManufacturer())
                .modelName(c.getModelName())
                .vin(c.getVin())
                .manufactureYear(c.getManufactureYear())
                .firstRegisterDate(c.getFirstRegisterDate())
                .mileage(c.getMileage())
                .inspectorCode(c.getInspectorCode())
                .inspectorName(c.getInspectorName())
                .issuedBy(c.getIssuedBy())
                .pdfFilePath(c.getPdfUrl())
                .build();
    }

    private String genCert() {
        String d = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String r = UUID.randomUUID().toString().replace("-", "").substring(0,6).toUpperCase();
        return "CERT-" + d + "-" + r;
    }

    private String date(LocalDate d) {
        return d == null ? "" : d.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"));
    }

    private String num(Number n) {
        return n == null ? "" : n.toString();
    }
}
