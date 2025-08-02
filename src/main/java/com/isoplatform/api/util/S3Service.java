package com.isoplatform.api.util;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cloudfront.distribution-domain}")
    private String cloudFrontDomain;

    private final AmazonS3Client amazonS3Client;

    /**
     * 파일을 S3에 업로드하고 CloudFront URL을 반환합니다.
     *
     * @param localFilePath 로컬 파일 경로
     * @param s3Key S3 키 (저장될 경로)
     * @return S3UploadResult 업로드 결과
     */
    public S3UploadResult uploadFile(String localFilePath, String s3Key) {
        try {
            File file = new File(localFilePath);
            if (!file.exists()) {
                throw new IllegalArgumentException("파일이 존재하지 않습니다: " + localFilePath);
            }

            // 메타데이터 설정
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            metadata.setContentType(getContentType(localFilePath));

            // S3에 업로드
            try (FileInputStream inputStream = new FileInputStream(file)) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, s3Key, inputStream, metadata);
                amazonS3Client.putObject(putObjectRequest);
            }

            // CloudFront URL 생성
            String cloudFrontUrl = generateCloudFrontUrl(s3Key);

            log.info("S3 업로드 완료 - S3 Key: {}, CloudFront URL: {}", s3Key, cloudFrontUrl);

            return S3UploadResult.builder()
                    .s3Key(s3Key)
                    .cloudFrontUrl(cloudFrontUrl)
                    .build();

        } catch (Exception e) {
            log.error("S3 업로드 중 오류 발생 - 파일: {}, S3 키: {}", localFilePath, s3Key, e);
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * CloudFront URL을 생성합니다.
     *
     * @param s3Key S3 키
     * @return CloudFront URL
     */
    private String generateCloudFrontUrl(String s3Key) {
        return "https://" + cloudFrontDomain + "/" + s3Key;
    }

    /**
     * 파일 확장자에 따른 Content-Type을 반환합니다.
     *
     * @param filePath 파일 경로
     * @return Content-Type
     */
    private String getContentType(String filePath) {
        String fileName = filePath.toLowerCase();
        if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream";
    }

    /**
     * 로컬 파일을 삭제합니다.
     *
     * @param localFilePath 삭제할 로컬 파일 경로
     */
    public void deleteLocalFile(String localFilePath) {
        try {
            Files.deleteIfExists(Paths.get(localFilePath));
            log.info("로컬 파일 삭제 완료: {}", localFilePath);
        } catch (IOException e) {
            log.warn("로컬 파일 삭제 실패: {}", localFilePath, e);
        }
    }

    /**
     * S3에서 파일을 삭제합니다.
     *
     * @param s3Key 삭제할 S3 키
     */
    public void deleteS3File(String s3Key) {
        try {
            amazonS3Client.deleteObject(bucket, s3Key);
            log.info("S3 파일 삭제 완료: {}", s3Key);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", s3Key, e);
            throw new RuntimeException("S3 파일 삭제 실패: " + e.getMessage(), e);
        }
    }
}
