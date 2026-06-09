package com.finflow.kyc.application;

public record ReviewKycCommand(
    KycDecisionAction decision,
    String reason
) {
}
