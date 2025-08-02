package com.isoplatform.api.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class PDFParser {

    /**
     * PDF 파일에서 전체 텍스트 내용을 추출합니다.
     *
     * @param inputStream PDF 파일 InputStream
     * @return 추출된 PDF 전체 텍스트
     */
    public String parseFullText(InputStream inputStream) {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            log.debug("PDF 텍스트 추출(앞 500자): {}", text.substring(0, Math.min(500, text.length())));

            return text;
        } catch (IOException e) {
            log.error("PDF 텍스트 추출 중 오류 발생", e);
            throw new RuntimeException("PDF 텍스트 추출 중 오류가 발생했습니다.", e);
        }
    }
}
