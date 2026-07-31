package com.backend.fourth.incident;

import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.repository.ExamSessionRepository;
import com.backend.fourth.incident.dto.CreateIncidentRequest;
import com.backend.fourth.incident.dto.IncidentResponse;
import com.backend.fourth.incident.entity.Incident;
import com.backend.fourth.incident.entity.IncidentType;
import com.backend.fourth.incident.repository.IncidentRepository;
import com.backend.fourth.incident.service.IncidentService;
import com.backend.fourth.invigilator.repository.InvigilatorAssignmentRepository;
import com.backend.fourth.staff.entity.Staff;
import com.backend.fourth.student.repository.StudentRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;
    @Mock
    private ExamSessionRepository examSessionRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private InvigilatorAssignmentRepository assignmentRepository;

    @InjectMocks
    private IncidentService incidentService;

    @Test
    void shouldReportIncidentForAssignedInvigilator() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                1, 2, null, "CHEATING", "Phone found under desk", "MAJOR", null);
        Staff staff = new Staff();
        staff.setStaffId(5);
        staff.setFullName("T. Mwewa");

        ExamSession exam = new ExamSession();
        exam.setExamSessionId(1);
        Venue venue = new Venue();
        venue.setVenueId(2);

        when(assignmentRepository.existsByExamSessionIdAndVenueIdAndStaffId(1, 2, 5)).thenReturn(true);
        when(examSessionRepository.findById(1)).thenReturn(Optional.of(exam));
        when(venueRepository.findById(2)).thenReturn(Optional.of(venue));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            incident.setIncidentId(99);
            return incident;
        });

        IncidentResponse response = incidentService.report(request, staff);

        assertEquals(99, response.incidentId());
        assertEquals("CHEATING", response.incidentType());
        assertEquals("MAJOR", response.severity());
        verify(incidentRepository).save(any(Incident.class));
    }
}
