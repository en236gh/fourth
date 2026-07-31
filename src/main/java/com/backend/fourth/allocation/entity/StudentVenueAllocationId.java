package com.backend.fourth.allocation.entity;

import java.io.Serializable;
import java.util.Objects;

public class StudentVenueAllocationId implements Serializable {
    private String computerNumber;
    private Integer examSessionId;

    public StudentVenueAllocationId() {}

    public StudentVenueAllocationId(String computerNumber, Integer examSessionId) {
        this.computerNumber = computerNumber;
        this.examSessionId = examSessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentVenueAllocationId that)) return false;
        return Objects.equals(computerNumber, that.computerNumber) && Objects.equals(examSessionId, that.examSessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(computerNumber, examSessionId);
    }
}
