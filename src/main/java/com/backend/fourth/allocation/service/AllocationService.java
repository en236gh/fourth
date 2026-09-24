package com.backend.fourth.allocation.service;

import com.backend.fourth.allocation.dto.AllocationStatsResponse;
import com.backend.fourth.allocation.entity.StudentVenueAllocation;
import com.backend.fourth.allocation.repository.StudentVenueAllocationRepository;
import com.backend.fourth.exam.entity.ExamSession;
import com.backend.fourth.exam.entity.ExamVenue;
import com.backend.fourth.exam.repository.ExamVenueRepository;
import com.backend.fourth.exam.service.LecturerCourseAccess;
import com.backend.fourth.student.entity.Student;
import com.backend.fourth.student.repository.StudentRegistrationRepository;
import com.backend.fourth.student.repository.StudentRepository;
import com.backend.fourth.venue.entity.Venue;
import com.backend.fourth.venue.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AllocationService {
    private final StudentRegistrationRepository registrationRepository;
    private final StudentRepository studentRepository;
    private final VenueRepository venueRepository;
    private final ExamVenueRepository examVenueRepository;
    private final StudentVenueAllocationRepository allocationRepository;
    private final LecturerCourseAccess lecturerCourseAccess;

    @Transactional(readOnly = true)
    public AllocationStatsResponse getAllocationStats(ExamSession examSession) {
        lecturerCourseAccess.requireReadAccess(examSession);
        long registered = registrationRepository.countByCourseCodeAndAcademicYearAndSemester(
                examSession.getCourseCode(), examSession.getAcademicYear(), examSession.getSemester());

        List<ExamVenue> examVenues = examVenueRepository.findByExamSessionIdOrderByVenueIdAsc(examSession.getExamSessionId());
        List<StudentVenueAllocation> allocations = allocationRepository.findByExamSessionId(examSession.getExamSessionId());

        Map<String, Student> studentsByComputer = new HashMap<>();
        Map<Integer, Venue> venuesById = new HashMap<>();
        long totalCapacity = 0;

        List<AllocationStatsResponse.VenueFillStats> fills = new ArrayList<>();
        for (ExamVenue examVenue : examVenues) {
            Venue venue = venueRepository.findById(examVenue.getVenueId())
                    .orElseThrow(() -> new IllegalArgumentException("Venue not found: " + examVenue.getVenueId()));
            venuesById.put(venue.getVenueId(), venue);
            totalCapacity += venue.getCapacity();
            long allocatedToVenue = allocations.stream()
                    .filter(allocation -> venue.getVenueId().equals(allocation.getVenueId()))
                    .count();
            fills.add(new AllocationStatsResponse.VenueFillStats(
                    venue.getVenueId(), venue.getVenueName(), venue.getCapacity(), allocatedToVenue));
        }

        List<AllocationStatsResponse.AllocationItem> items = new ArrayList<>();
        for (StudentVenueAllocation allocation : allocations) {
            Student student = studentsByComputer.computeIfAbsent(
                    allocation.getComputerNumber(),
                    computerNumber -> studentRepository.findByComputerNumber(computerNumber).orElse(null));
            Venue venue = venuesById.computeIfAbsent(
                    allocation.getVenueId(),
                    venueId -> venueRepository.findById(venueId).orElse(null));
            items.add(new AllocationStatsResponse.AllocationItem(
                    allocation.getComputerNumber(),
                    student != null ? student.getFullName() : null,
                    allocation.getVenueId(),
                    venue != null ? venue.getVenueName() : null));
        }

        return new AllocationStatsResponse(
                examSession.getExamSessionId(),
                registered,
                allocations.size(),
                totalCapacity,
                fills,
                items);
    }

}
