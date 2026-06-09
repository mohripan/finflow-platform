package com.finflow.kyc.application;

import java.time.Instant;

public interface DocumentStoragePort {
  PresignedUpload createUpload(String objectKey, String contentType);
  StoredObject verifyUploaded(String objectKey, long expectedSizeBytes);
  PresignedDownload createReviewUrl(String objectKey);

  record PresignedUpload(String bucket, String objectKey, String uploadUrl, Instant expiresAt) {
  }

  record PresignedDownload(String reviewUrl, Instant expiresAt) {
  }

  record StoredObject(long sizeBytes, String contentType) {
  }
}
