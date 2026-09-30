package com.backend.fourth.scheduling;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static com.backend.fourth.scheduling.ConstraintScheduler.*;
import static org.junit.jupiter.api.Assertions.*;

class ConstraintSchedulerTest {
    private final LocalDateTime nine=LocalDateTime.of(2090,1,2,9,0);
    private Slot slot(int hour) { return new Slot(nine.plusHours(hour),nine.plusHours(hour+2)); }
    private Exam exam(String code,String... students) { return new Exam(code,120,Set.of(students)); }
    private ConstraintScheduler scheduler(List<Slot> slots,List<Room> rooms,List<Placement> booked,int limit) { return new ConstraintScheduler(slots,rooms,booked,limit); }

    @Test void simultaneousExamsUseDifferentVenues() {
        var result=scheduler(List.of(slot(0)),List.of(new Room(1,1),new Room(2,1)),List.of(),100).solve(List.of(exam("A","one"),exam("B","two")));
        assertEquals(Outcome.COMPLETE,result.outcome());
        assertEquals(result.placements().get(0).start(),result.placements().get(1).start());
        assertNotEquals(result.placements().get(0).venues(),result.placements().get(1).venues());
    }
    @Test void sharedStudentsRequireSeparateIntervalsAndVenueCanBeReused() {
        var result=scheduler(List.of(slot(0),slot(2)),List.of(new Room(1,2)),List.of(),100).solve(List.of(exam("A","one"),exam("B","one")));
        assertEquals(Outcome.COMPLETE,result.outcome());
        assertEquals(result.placements().get(0).end(),result.placements().get(1).start());
        assertEquals(result.placements().get(0).venues(),result.placements().get(1).venues());
    }
    @Test void splitsLargeCourseAcrossRoomsWithoutDuplicatingStudents() {
        var result=scheduler(List.of(slot(0)),List.of(new Room(1,2),new Room(2,2)),List.of(),100).solve(List.of(exam("A","1","2","3")));
        assertEquals(Outcome.COMPLETE,result.outcome());
        assertEquals(List.of(1,2),result.placements().getFirst().venues());
        assertEquals(3,result.placements().getFirst().students().size());
    }
    @Test void respectsReservationsWhoseStartTimesDiffer() {
        Placement reservation=new Placement("external",nine.plusMinutes(30),nine.plusHours(3),List.of(1),Set.of());
        assertEquals(Outcome.NO_FEASIBLE_ARRANGEMENT,scheduler(List.of(slot(0)),List.of(new Room(1,100)),List.of(reservation),100).solve(List.of(exam("A","1"))).outcome());
    }
    @Test void checksCrossCourseExternalStudentClashesWithoutVenueBooking() {
        Placement reservation=new Placement("external",nine.plusMinutes(30),nine.plusHours(3),List.of(),Set.of("1"));
        assertEquals(Outcome.NO_FEASIBLE_ARRANGEMENT,scheduler(List.of(slot(0)),List.of(new Room(1,100)),List.of(reservation),100).solve(List.of(exam("A","1"))).outcome());
    }
    @Test void rejectsInsufficientCapacityAndDuration() {
        assertEquals(Outcome.NO_FEASIBLE_ARRANGEMENT,scheduler(List.of(slot(0)),List.of(new Room(1,1)),List.of(),100).solve(List.of(exam("A","1","2"))).outcome());
        var duration=scheduler(List.of(slot(0)),List.of(new Room(1,100)),List.of(),100).solve(List.of(new Exam("A",121,Set.of("1"))));
        assertEquals(Outcome.INVALID_INPUT,duration.outcome());
        assertTrue(duration.problems().getFirst().contains("121"));
        assertTrue(duration.problems().getFirst().contains("120"));
    }
    @Test void onlyOneSlotNeedsToFitDuration() {
        var result=scheduler(List.of(new Slot(nine,nine.plusMinutes(30)),slot(2)),List.of(new Room(1,1)),List.of(),100)
                .solve(List.of(exam("A","1")));
        assertEquals(Outcome.COMPLETE,result.outcome());
        assertEquals(nine.plusHours(2),result.placements().getFirst().start());
    }
    @Test void searchLimitDoesNotClaimInfeasibilityOrReturnPartialSchedule() {
        var result=scheduler(List.of(slot(0)),List.of(new Room(1,10)),List.of(),1).solve(List.of(exam("A","1")));
        assertEquals(Outcome.SEARCH_LIMIT_REACHED,result.outcome());
        assertTrue(result.placements().isEmpty());
        assertEquals(1,result.searchSteps());
        assertTrue(result.problems().getFirst().contains("unknown"));
    }
    @Test void backtracksRoomChoiceToLeaveOnlySuitableRoomForLongExam() {
        // A is prioritized by student count. Its first room choice blocks B's long
        // exam; B cannot use room 2, reserved during the second hour. A must move.
        var blocked=new Placement("external",nine.plusHours(1),nine.plusHours(2),List.of(2),Set.of());
        var a=new Exam("A",60,Set.of("1","2"));
        var b=new Exam("B",120,Set.of("3"));
        var s=scheduler(List.of(slot(0)),List.of(new Room(1,2),new Room(2,2)),List.of(blocked),1000);
        var result=s.solve(List.of(a,b));
        assertEquals(Outcome.COMPLETE,result.outcome());
        assertEquals(List.of(2),result.placements().getFirst().venues());
        assertEquals(List.of(1),result.placements().getLast().venues());
        assertEquals(result,s.solve(List.of(b,a)));
    }
    @Test void independentValidationRejectsBadManualPlacement() {
        var s=scheduler(List.of(slot(0)),List.of(new Room(1,1)),List.of(),100);
        var problems=s.validate(List.of(exam("A","1")),List.of(new Placement("A",nine,nine.plusHours(3),List.of(1),Set.of("other"))));
        assertEquals(2,problems.size());
    }
    @Test void invalidInputsHaveDistinctOutcome() {
        assertEquals(Outcome.INVALID_INPUT,scheduler(List.of(slot(0)),List.of(),List.of(),100).solve(List.of(exam("A","1"))).outcome());
        assertEquals(Outcome.INVALID_INPUT,scheduler(List.of(slot(0)),List.of(new Room(1,1)),List.of(),100).solve(List.of(exam("A"))).outcome());
    }
}
