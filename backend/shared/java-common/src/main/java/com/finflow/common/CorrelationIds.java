package com.finflow.common;

import java.util.UUID;

public final class CorrelationIds {
  public static final String HEADER = "X-Correlation-Id";

  private CorrelationIds() {
  }

  public static String ensure(String candidate) {
    return candidate == null || candidate.isBlank() ? UUID.randomUUID().toString() : candidate;
  }
}
