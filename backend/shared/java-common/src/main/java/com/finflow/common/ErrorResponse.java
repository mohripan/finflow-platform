package com.finflow.common;

public record ErrorResponse(ApiError error, ApiMeta meta) {
  public static ErrorResponse of(String code, String message, String correlationId) {
    return new ErrorResponse(ApiError.of(code, message), new ApiMeta(correlationId));
  }
}
