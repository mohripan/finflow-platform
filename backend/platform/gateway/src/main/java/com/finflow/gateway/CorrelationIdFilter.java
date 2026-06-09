package com.finflow.gateway;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CorrelationIdFilter {
  static final String HEADER = "X-Correlation-Id";

  @Bean
  GlobalFilter correlationIdGlobalFilter() {
    return (exchange, chain) -> {
      var request = exchange.getRequest();
      var correlationId = request.getHeaders().getFirst(HEADER);
      if (correlationId == null || correlationId.isBlank()) {
        correlationId = UUID.randomUUID().toString();
      }
      var mutated = request.mutate().header(HEADER, correlationId).build();
      exchange.getResponse().getHeaders().set(HEADER, correlationId);
      return chain.filter(exchange.mutate().request(mutated).build());
    };
  }
}
