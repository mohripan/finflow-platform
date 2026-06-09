package com.finflow.user.api;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 120) String displayName,
    @Size(max = 16) String preferredLanguage
) {
}
