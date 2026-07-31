package com.backend.fourth.invigilator.entity;

import java.io.Serializable;
import java.util.Objects;

public class InvigilatorAssignmentId implements Serializable {
    private Integer examSessionId;
    private Integer venueId;
    private Integer staffId;

    public InvigilatorAssignmentId() {
    }

    public InvigilatorAssignmentId(Integer examSessionId, Integer venueId, Integer staffId) {
        this.examSessionId = examSessionId;
        this.venueId = venueId;
        this.staffId = staffId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InvigilatorAssignmentId that)) {
            return false;
        }
        return Objects.equals(examSessionId, that.examSessionId)
                && Objects.equals(venueId, that.venueId)
                && Objects.equals(staffId, that.staffId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(examSessionId, venueId, staffId);
    }
}
