package com.finflow.user.api;

import com.finflow.user.domain.CustomerAccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SyncCustomerStatusRequest(
    @NotBlank String keycloakSubject,
    @NotNull CustomerAccountStatus status
) {
}
