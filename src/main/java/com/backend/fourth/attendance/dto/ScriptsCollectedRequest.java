package com.backend.fourth.attendance.dto;

import jakarta.validation.constraints.NotNull;

public record ScriptsCollectedRequest(@NotNull Integer count) {
}
