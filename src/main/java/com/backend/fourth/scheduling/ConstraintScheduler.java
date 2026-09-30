package com.backend.fourth.scheduling;

import java.time.LocalDateTime;
import java.util.*;

/** Pure, deterministic search. No database writes occur until a complete result is validated. */
public final class ConstraintScheduler {
    public enum Outcome { COMPLETE, INVALID_INPUT, NO_FEASIBLE_ARRANGEMENT, SEARCH_LIMIT_REACHED }
    public record Slot(LocalDateTime start, LocalDateTime end) {}
    public record Room(int id, int capacity) {}
    public record Exam(String course, int minutes, Set<String> students) {}
    public record Placement(String course, LocalDateTime start, LocalDateTime end,
                            List<Integer> venues, Set<String> students) {}
    public record Result(Outcome outcome, List<Placement> placements, List<String> unresolvedCourses,
                         long searchSteps, List<String> problems) {}
    private final List<Slot> slots;
    private final List<Room> rooms;
    private final List<Placement> reservations;
    private final int limit;
    private long steps;
    private boolean exhausted;
    private List<Placement> best = List.of();

    public ConstraintScheduler(List<Slot> slots, List<Room> rooms, List<Placement> reservations, int limit) {
        this.slots = slots.stream().sorted(Comparator.comparing(Slot::start).thenComparing(Slot::end)).toList();
        this.rooms = rooms.stream().sorted(Comparator.comparingInt(Room::id)).toList();
        this.reservations = List.copyOf(reservations);
        this.limit = limit;
    }

    public Result solve(List<Exam> exams) {
        steps = 0; exhausted = false; best = List.of();
        List<String> input = new ArrayList<>();
        if (limit < 1 || limit > 1_000_000) input.add("Search limit must be between 1 and 1000000.");
        if (exams.isEmpty() || exams.size() > 100) input.add("Select between 1 and 100 courses.");
        if (slots.isEmpty()) input.add("Define at least one dated slot.");
        if (rooms.isEmpty()) input.add("Configure at least one positive examination seating capacity.");
        if (rooms.stream().anyMatch(r -> r.capacity() <= 0)) input.add("Examination capacities must be positive.");
        if (rooms.stream().map(Room::id).distinct().count() != rooms.size()) input.add("Duplicate venue IDs.");
        if (slots.stream().anyMatch(s -> !s.start().isBefore(s.end()))) input.add("Slot ends must follow starts.");
        if (exams.stream().map(Exam::course).distinct().count() != exams.size()) input.add("Duplicate selected courses.");
        for (Exam exam : exams) {
            if (exam.minutes() < 1 || exam.minutes() > 1440) input.add(exam.course() + ": invalid duration.");
            if (exam.minutes() > 0 && slots.stream().noneMatch(slot -> !slot.start().plusMinutes(exam.minutes()).isAfter(slot.end())))
                input.add(exam.course() + ": duration " + exam.minutes() + " minutes does not fit any slot. Available slot lengths (minutes): "
                        + slots.stream().map(slot -> java.time.Duration.between(slot.start(), slot.end()).toMinutes()).distinct().sorted().toList()
                        + ". Extend/add a slot or correct the duration; the exam will not be shortened.");
            if (exam.students().isEmpty()) input.add(exam.course() + ": no eligible course registrations.");
        }
        if (!input.isEmpty()) return new Result(Outcome.INVALID_INPUT, List.of(), courses(exams), steps, input);
        long totalCapacity = rooms.stream().mapToLong(Room::capacity).sum();
        List<String> shortages = exams.stream().filter(e -> e.students().size() > totalCapacity)
                .map(e -> e.course() + ": needs " + e.students().size() + " seats; configured capacity " + totalCapacity
                        + "; shortage " + (e.students().size() - totalCapacity)
                        + ". Add available venues or verified seating capacity. Extra slots alone cannot seat a simultaneous exam.").toList();
        if (!shortages.isEmpty()) return new Result(Outcome.NO_FEASIBLE_ARRANGEMENT, List.of(), courses(exams), 0, shortages);
        List<Exam> ordered = exams.stream().sorted(
                Comparator.<Exam>comparingLong(e -> availableSlots(e)).thenComparing(
                        Comparator.<Exam>comparingLong(e -> exams.stream().filter(other -> other != e
                                && !Collections.disjoint(e.students(), other.students())).count()).reversed())
                        .thenComparing(Comparator.<Exam>comparingInt(e -> e.students().size()).reversed())
                        .thenComparing(Exam::course)).toList();
        List<Placement> placed = new ArrayList<>();
        if (search(ordered, 0, placed)) {
            List<String> problems = validate(exams, placed);
            if (!problems.isEmpty()) throw new IllegalStateException("Scheduler validation failed: " + problems);
            return new Result(Outcome.COMPLETE, List.copyOf(placed), List.of(), steps, List.of());
        }
        Set<String> resolved = new HashSet<>();
        best.forEach(p -> resolved.add(p.course()));
        List<String> unresolved = exams.stream().map(Exam::course).filter(c -> !resolved.contains(c)).sorted().toList();
        return new Result(exhausted ? Outcome.SEARCH_LIMIT_REACHED : Outcome.NO_FEASIBLE_ARRANGEMENT,
                List.of(), unresolved, steps, List.of(exhausted
                ? "Search limit reached; feasibility is unknown. Increase the search limit, add venues or slots, or extend the period."
                : "All allowed placements were exhausted. Add examination capacity or slots, or extend the period."));
    }

    private long availableSlots(Exam e) {
        return slots.stream().filter(s -> !s.start().plusMinutes(e.minutes()).isAfter(s.end()))
                .filter(s -> reservations.stream().noneMatch(p -> overlaps(s.start(), s.start().plusMinutes(e.minutes()), p)
                        && !Collections.disjoint(e.students(), p.students())))
                .filter(s -> rooms.stream().filter(r -> reservations.stream().noneMatch(p ->
                        p.venues().contains(r.id()) && overlaps(s.start(), s.start().plusMinutes(e.minutes()), p)))
                        .mapToLong(Room::capacity).sum() >= e.students().size()).count();
    }

    private boolean tick() {
        if (steps >= limit) { exhausted = true; return false; }
        steps++; return true;
    }

    private boolean search(List<Exam> exams, int index, List<Placement> placed) {
        if (placed.size() > best.size()) best = List.copyOf(placed);
        if (index == exams.size()) return true;
        Exam exam = exams.get(index);
        for (Slot slot : slots) {
            if (!tick()) return false;
            LocalDateTime end = slot.start().plusMinutes(exam.minutes());
            if (end.isAfter(slot.end())) continue;
            List<Placement> occupied = new ArrayList<>(reservations); occupied.addAll(placed);
            if (occupied.stream().anyMatch(p -> overlaps(slot.start(), end, p)
                    && !Collections.disjoint(exam.students(), p.students()))) continue;
            List<Room> available = rooms.stream().filter(r -> occupied.stream().noneMatch(p ->
                    overlaps(slot.start(), end, p) && p.venues().contains(r.id()))).toList();
            if (available.stream().mapToLong(Room::capacity).sum() < exam.students().size()) continue;
            if (chooseRooms(exams, index, placed, slot.start(), end, available, 0, new ArrayList<>(), 0)) return true;
            if (exhausted) return false;
        }
        return false;
    }

    private boolean chooseRooms(List<Exam> exams, int index, List<Placement> placed, LocalDateTime start,
                                LocalDateTime end, List<Room> available, int from, List<Integer> selected, long capacity) {
        if (!tick()) return false;
        Exam exam = exams.get(index);
        if (capacity >= exam.students().size()) {
            placed.add(new Placement(exam.course(), start, end, List.copyOf(selected), exam.students()));
            if (search(exams, index + 1, placed)) return true;
            placed.removeLast();
            return false; // Extra rooms can only remove options for remaining examinations.
        }
        for (int i = from; i < available.size(); i++) {
            Room room = available.get(i); selected.add(room.id());
            if (chooseRooms(exams, index, placed, start, end, available, i + 1, selected, capacity + room.capacity())) return true;
            selected.removeLast();
            if (exhausted) return false;
        }
        return false;
    }

    /** Independent validation, also used for manual edits and before publication. */
    public List<String> validate(List<Exam> exams, List<Placement> placements) {
        List<String> problems = new ArrayList<>();
        Map<Integer, Integer> capacities = new HashMap<>(); rooms.forEach(r -> capacities.put(r.id(), r.capacity()));
        for (Exam exam : exams) {
            List<Placement> matches = placements.stream().filter(p -> p.course().equals(exam.course())).toList();
            if (matches.size() != 1) { problems.add(exam.course() + ": requires exactly one session."); continue; }
            Placement p = matches.getFirst();
            if (!p.students().equals(exam.students())) problems.add(exam.course() + ": allocations differ from eligible registrations.");
            if (!p.end().equals(p.start().plusMinutes(exam.minutes())) || slots.stream().noneMatch(s ->
                    !p.start().isBefore(s.start()) && !p.end().isAfter(s.end()))) problems.add(exam.course() + ": outside allowed slots or wrong duration.");
            if (new HashSet<>(p.venues()).size() != p.venues().size() || p.venues().isEmpty()
                    || p.venues().stream().anyMatch(v -> !capacities.containsKey(v))
                    || p.venues().stream().mapToLong(v -> capacities.getOrDefault(v, 0)).sum() < exam.students().size())
                problems.add(exam.course() + ": invalid venues or insufficient examination capacity.");
        }
        if (placements.size() != exams.size() || placements.stream().anyMatch(p -> exams.stream().noneMatch(e -> e.course().equals(p.course()))))
            problems.add("Unexpected or missing examinations.");
        for (int i = 0; i < placements.size(); i++) {
            Placement p = placements.get(i);
            List<Placement> others = new ArrayList<>(reservations); others.addAll(placements.subList(i + 1, placements.size()));
            for (Placement other : others) if (overlaps(p.start(), p.end(), other)) {
                if (!Collections.disjoint(p.venues(), other.venues())) problems.add(p.course() + ": venue overlap with " + other.course());
                if (!Collections.disjoint(p.students(), other.students())) problems.add(p.course() + ": student overlap with " + other.course());
            }
        }
        return problems;
    }

    public static boolean overlaps(LocalDateTime start, LocalDateTime end, Placement p) {
        return start.isBefore(p.end()) && end.isAfter(p.start());
    }
    private static List<String> courses(List<Exam> exams) { return exams.stream().map(Exam::course).sorted().toList(); }
}
