package com.finflow.user.application;

public record UpdateProfileCommand(
    String displayName,
    String preferredLanguage
) {
}
