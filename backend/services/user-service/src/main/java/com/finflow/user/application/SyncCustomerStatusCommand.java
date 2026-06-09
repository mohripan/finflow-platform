package com.finflow.user.application;

import com.finflow.user.domain.CustomerAccountStatus;

public record SyncCustomerStatusCommand(
    String keycloakSubject,
    CustomerAccountStatus status
) {
}
