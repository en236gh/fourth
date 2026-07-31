package com.backend.fourth.exam.entity;

import java.io.Serializable;
import java.util.Objects;

public class ExamVenueId implements Serializable {
    private Integer examSessionId;
    private Integer venueId;

    public ExamVenueId() {
    }

    public ExamVenueId(Integer examSessionId, Integer venueId) {
        this.examSessionId = examSessionId;
        this.venueId = venueId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExamVenueId that)) {
            return false;
        }
        return Objects.equals(examSessionId, that.examSessionId)
                && Objects.equals(venueId, that.venueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(examSessionId, venueId);
    }
}
