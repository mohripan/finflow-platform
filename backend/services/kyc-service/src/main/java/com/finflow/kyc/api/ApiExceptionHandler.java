package com.finflow.kyc.api;

import com.finflow.common.CorrelationIds;
import com.finflow.common.ErrorResponse;
import com.finflow.kyc.domain.BusinessRuleException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    var correlationId = CorrelationIds.ensure(request.getHeader(CorrelationIds.HEADER));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of("VALIDATION_ERROR", "Request field validation failed.", correlationId));
  }

  @ExceptionHandler(BusinessRuleException.class)
  ResponseEntity<ErrorResponse> business(BusinessRuleException ex, HttpServletRequest request) {
    var correlationId = CorrelationIds.ensure(request.getHeader(CorrelationIds.HEADER));
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(ErrorResponse.of(ex.code(), ex.getMessage(), correlationId));
  }
}
