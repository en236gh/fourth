package com.backend.fourth.invigilator.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record BulkPublishRequest(@NotEmpty List<@NotNull @Positive Integer> examSessionIds) {
}
