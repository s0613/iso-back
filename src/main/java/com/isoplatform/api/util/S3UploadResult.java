package com.isoplatform.api.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3UploadResult {
    private String s3Key;
    private String cloudFrontUrl;
}
