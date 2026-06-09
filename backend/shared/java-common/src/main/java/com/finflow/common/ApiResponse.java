package com.finflow.common;

public record ApiResponse<T>(T data, ApiMeta meta) {
  public static <T> ApiResponse<T> ok(T data, String correlationId) {
    return new ApiResponse<>(data, new ApiMeta(correlationId));
  }
}
