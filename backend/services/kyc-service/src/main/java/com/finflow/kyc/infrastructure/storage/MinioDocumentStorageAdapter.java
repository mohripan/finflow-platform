package com.finflow.kyc.infrastructure.storage;

import com.finflow.kyc.application.DocumentStoragePort;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class MinioDocumentStorageAdapter implements DocumentStoragePort {
  private final MinioClient client;
  private final String bucket;
  private final int expirySeconds;

  MinioDocumentStorageAdapter(
      @Value("${finflow.kyc.documents.endpoint:http://localhost:9000}") String endpoint,
      @Value("${finflow.kyc.documents.access-key:minioadmin}") String accessKey,
      @Value("${finflow.kyc.documents.secret-key:minioadmin}") String secretKey,
      @Value("${finflow.kyc.documents.bucket:finflow-kyc}") String bucket,
      @Value("${finflow.kyc.documents.presign-expiry-seconds:900}") int expirySeconds) {
    this.client = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build();
    this.bucket = bucket;
    this.expirySeconds = expirySeconds;
    ensureBucket();
  }

  @Override
  public PresignedUpload createUpload(String objectKey, String contentType) {
    return new PresignedUpload(bucket, objectKey, presign(Method.PUT, objectKey), Instant.now().plusSeconds(expirySeconds));
  }

  @Override
  public StoredObject verifyUploaded(String objectKey, long expectedSizeBytes) {
    try {
      var stat = client.statObject(StatObjectArgs.builder()
          .bucket(bucket)
          .object(objectKey)
          .build());
      if (stat.size() != expectedSizeBytes) {
        throw new IllegalStateException("KYC document object size does not match the upload session.");
      }
      return new StoredObject(stat.size(), stat.contentType());
    } catch (Exception ex) {
      if (ex instanceof IllegalStateException) {
        throw (IllegalStateException) ex;
      }
      throw new IllegalStateException("Unable to verify uploaded KYC document object.", ex);
    }
  }

  @Override
  public PresignedDownload createReviewUrl(String objectKey) {
    return new PresignedDownload(presign(Method.GET, objectKey), Instant.now().plusSeconds(expirySeconds));
  }

  private void ensureBucket() {
    try {
      var exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
      if (!exists) {
        client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
      }
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to prepare KYC document bucket.", ex);
    }
  }

  private String presign(Method method, String objectKey) {
    try {
      return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
          .method(method)
          .bucket(bucket)
          .object(objectKey)
          .expiry(expirySeconds, TimeUnit.SECONDS)
          .build());
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to generate KYC document signed URL.", ex);
    }
  }
}
