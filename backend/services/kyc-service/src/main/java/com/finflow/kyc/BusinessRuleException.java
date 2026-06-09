package com.finflow.kyc;

class BusinessRuleException extends RuntimeException {
  private final String code;

  BusinessRuleException(String code, String message) {
    super(message);
    this.code = code;
  }

  String code() {
    return code;
  }
}
