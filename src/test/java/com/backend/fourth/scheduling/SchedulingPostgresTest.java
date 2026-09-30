package com.backend.fourth.scheduling;

import com.backend.fourth.invigilator.repository.AssignmentWriteLock;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in; connects only to the disposable local cluster documented in the guide. */
@EnabledIfSystemProperty(named="scheduling.it",matches="true")
class SchedulingPostgresTest {
    private static final String ROOT="jdbc:postgresql://127.0.0.1:55439/";
    private String database;
    private JdbcTemplate jdbc;
    private SchedulingService service;
    private AnnotationConfigApplicationContext context;
    @Configuration @EnableTransactionManagement static class Transactions {}

    @BeforeEach void prepare() throws Exception {
        database="scheduling_it_"+UUID.randomUUID().toString().replace("-","");
        try(var c=DriverManager.getConnection(ROOT+"postgres",System.getProperty("user.name"),"");var s=c.createStatement()) { s.execute("CREATE DATABASE "+database); }
        var ds=new DriverManagerDataSource(ROOT+database,System.getProperty("user.name"),"");
        jdbc=new JdbcTemplate(ds);
        executeResource("/scheduling-base.sql");
        // A stale legacy allocation exists before migration and must survive, visibly audited.
        jdbc.update("INSERT INTO course VALUES ('OLD','Legacy',true)");
        jdbc.update("INSERT INTO student VALUES ('legacy','Legacy student')");
        jdbc.update("INSERT INTO venue(venue_id,venue_name,building,capacity) VALUES (99,'Legacy','Old',100)");
        jdbc.update("INSERT INTO exam_session(course_code,exam_date,start_time,end_time,academic_year,semester,exam_type) VALUES ('OLD','2080-01-01','09:00','11:00','2080/2081',1,'FINAL')");
        jdbc.update("INSERT INTO student_venue_allocation VALUES ('legacy',1,99)");
        executeResource("/db/migration/V32__examination_scheduling.sql");
        executeResource("/db/migration/V33__scheduling_coordination.sql");
        executeResource("/db/migration/V34__scheduling_change_requests.sql");
        executeResource("/db/migration/V35__published_time_amendments.sql");
        executeResource("/db/migration/V36__scheduling_integrity_hardening.sql");
        executeResource("/db/migration/V37__approved_placement_amendments.sql");
        executeResource("/db/migration/V38__administrator_owned_scheduling.sql");
        executeResource("/db/migration/V39__allow_multiple_draft_periods.sql");
        jdbc.update("INSERT INTO course VALUES ('A','One student',true),('B','Other students',true),('C','Shared student',true),('D','Large course',true)");
        jdbc.update("INSERT INTO student VALUES ('1','One'),('2','Two'),('3','Three'),('4','Four')");
        jdbc.update("INSERT INTO student_registration VALUES ('1','A','2090/2091',1),('2','B','2090/2091',1),('1','C','2090/2091',1),('1','D','2090/2091',1),('2','D','2090/2091',1),('3','D','2090/2091',1)");
        jdbc.update("INSERT INTO venue(venue_id,venue_name,building,capacity,examination_capacity) VALUES (1,'Room 1','Main',100,2),(2,'Room 2','Main',100,2)");
        jdbc.update("INSERT INTO staff VALUES (1,'ACTIVE'),(2,'ACTIVE')");
        jdbc.update("INSERT INTO role VALUES (1,'INVIGILATOR'),(2,'ADMINISTRATOR')");
        jdbc.update("INSERT INTO staff_role VALUES (1,1),(2,1),(1,2)");
        context=new AnnotationConfigApplicationContext();
        context.register(Transactions.class);
        context.registerBean(JdbcTemplate.class,()->jdbc);
        context.registerBean(org.springframework.transaction.PlatformTransactionManager.class,()->new DataSourceTransactionManager(ds));
        context.registerBean(AssignmentWriteLock.class);
        var resolver=org.mockito.Mockito.mock(com.backend.fourth.common.security.CurrentStaffResolver.class);
        var actor=new com.backend.fourth.staff.entity.Staff(); actor.setStaffId(1); actor.setAccountStatus("ACTIVE");
        var adminRole=new com.backend.fourth.staff.entity.Role(); adminRole.setName("ADMINISTRATOR"); actor.setRoles(Set.of(adminRole));
        org.mockito.Mockito.when(resolver.requireCurrentStaff()).thenReturn(actor);
        context.registerBean(com.backend.fourth.common.security.CurrentStaffResolver.class,()->resolver);
        context.registerBean(SchedulingDeniedAudit.class);
        context.registerBean(SchedulingAccess.class);
        context.registerBean(SchedulingReviewService.class);
        context.registerBean(SchedulingAmendmentService.class);
        context.registerBean(SchedulingNotificationService.class);
        context.registerBean(SchedulingNotificationListener.class);
        context.registerBean(SchedulingService.class);
        context.refresh();
        service=context.getBean(SchedulingService.class);
    }
    private void executeResource(String path) throws Exception {
        try(var in=getClass().getResourceAsStream(path);var c=jdbc.getDataSource().getConnection();var statement=c.createStatement()) {
            c.setAutoCommit(false);
            statement.execute(new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8));c.commit();
        }
    }
    @AfterEach void cleanup() throws Exception {
        if(context!=null) context.close();
        if(database!=null) try(var c=DriverManager.getConnection(ROOT+"postgres",System.getProperty("user.name"),"");var s=c.createStatement()) { s.execute("DROP DATABASE "+database+" WITH (FORCE)"); }
    }
    private int create(String... courses) {
        LocalDate day=LocalDate.of(2090,1,2);
        Map<String,Object> p=service.create(new SchedulingRequests.Period("Demo","2090/2091",1,"FINAL",day,day.plusDays(4),List.of(1,2,3,4,5,6,7),List.of(
                new SchedulingRequests.DailySlot(LocalTime.of(9,0),LocalTime.of(11,0)),new SchedulingRequests.DailySlot(LocalTime.of(11,0),LocalTime.of(13,0)))));
        int id=((Number)p.get("period_id")).intValue();
        service.select(id,new SchedulingRequests.Selection(0,Arrays.stream(courses).map(c->new SchedulingRequests.Course(c,120)).toList()));
        return id;
    }
    private SchedulingService.Generation generate(int id,long revision) { return service.generate(id,new SchedulingRequests.Generate(revision,100000)); }
    private int session(int id,String course) { return jdbc.queryForObject("SELECT exam_session_id FROM exam_session WHERE period_id=? AND course_code=?",Integer.class,id,course); }
    private long revision(int id) { return ((Number)service.detail(id).get("revision")).longValue(); }
    private void staffEveryBooking(int id) {
        jdbc.update("""
                INSERT INTO invigilator_assignment(exam_session_id,venue_id,staff_id)
                SELECT v.exam_session_id,v.venue_id,v.venue_id FROM exam_venue v JOIN exam_session e USING(exam_session_id) WHERE e.period_id=?
                """,id);
    }

    @Test void migrationPreservesAmbiguousLegacyRecordsAndDoesNotGuessCapacity() {
        assertEquals(1,service.audit().size());
        assertEquals("MISSING_EXAM_VENUE",service.audit().getFirst().get("issue"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM student_venue_allocation WHERE computer_number='legacy'",Integer.class));
        assertNull(jdbc.queryForObject("SELECT examination_capacity FROM venue WHERE venue_id=99",Integer.class));
        assertTrue(jdbc.queryForObject("SELECT schedule_published FROM exam_session WHERE exam_session_id=1",Boolean.class));
    }
    @Test void multipleDraftPeriodsCanShareCycleAndDeletingOneLeavesTheOtherIntact() {
        int first=create("A");
        int second=create("B");
        generate(first,1);generate(second,1);
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM examination_period WHERE academic_year='2090/2091' AND semester=1 AND exam_type='FINAL'",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM exam_session WHERE period_id IN (?,?)",Integer.class,first,second));
        service.deleteDraftPeriod(first,2);
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM examination_period WHERE period_id=?",Integer.class,first));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM exam_session WHERE period_id=?",Integer.class,second));
        int replacement=create("C");
        assertNotEquals(first,replacement);
    }
    @Test void deletingDraftRequiresCurrentRevisionAndRejectsPublishedPeriod() {
        int id=create("A");
        assertThrows(IllegalStateException.class,()->service.deleteDraftPeriod(id,0));
        generate(id,1);staffEveryBooking(id);service.publish(id,2);
        assertThrows(IllegalStateException.class,()->service.deleteDraftPeriod(id,3));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM examination_period WHERE period_id=?",Integer.class,id));
    }
    @Test void publishedCycleCourseBlocksOtherDraftFromPublishing() {
        int first=create("A");int second=create("A");
        generate(first,1);generate(second,1);
        staffEveryBooking(first);service.publish(first,2);
        staffEveryBooking(second);
        assertTrue(service.validate(second).problems().stream().anyMatch(problem->problem.contains("another legacy or published examination")));
        assertThrows(IllegalStateException.class,()->service.publish(second,2));
        assertEquals("DRAFT",service.detail(second).get("status"));
    }
    @Test void completeWorkflowGeneratesAllocatesValidatesAndPublishes() {
        int id=create("A","B","C","D");
        var result=generate(id,1);
        assertEquals(ConstraintScheduler.Outcome.COMPLETE,result.result().outcome());
        assertEquals(6,jdbc.queryForObject("SELECT count(*) FROM student_venue_allocation a JOIN exam_session e USING(exam_session_id) WHERE period_id=?",Integer.class,id));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM exam_venue WHERE exam_session_id=?",Integer.class,session(id,"D")));
        assertFalse(service.validate(id).valid());
        assertThrows(IllegalStateException.class,()->service.publish(id,2));
        assertEquals("DRAFT",service.detail(id).get("status"));
        staffEveryBooking(id);
        assertTrue(service.validate(id).valid(),service.validate(id).problems().toString());
        service.publish(id,2);
        assertEquals("PUBLISHED",service.detail(id).get("status"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM exam_session WHERE period_id=? AND NOT schedule_published",Integer.class,id));
        assertThrows(IllegalStateException.class,()->generate(id,3));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("DELETE FROM student_registration WHERE computer_number='1' AND course_code='A'"));
    }
    @Test void repeatedGenerationHasNoDuplicatesAndFailureKeepsPreviousDraft() {
        int id=create("A","B");generate(id,1);generate(id,2);
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM exam_session WHERE period_id=?",Integer.class,id));
        var before=service.detail(id).get("allocations");
        var failed=service.generate(id,new SchedulingRequests.Generate(3,1));
        assertEquals(ConstraintScheduler.Outcome.SEARCH_LIMIT_REACHED,failed.result().outcome());
        assertEquals(before,service.detail(id).get("allocations"));
        assertEquals(3,revision(id));
        assertThrows(IllegalStateException.class,()->generate(id,1));
    }
    @Test void rejectsInvalidCourseAllocationDuplicateAndBookingOverlap() {
        int id=create("A","B");generate(id,1);
        int a=session(id,"A"),b=session(id,"B");
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("INSERT INTO student_venue_allocation VALUES ('2',?,1)",a));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("INSERT INTO student_venue_allocation VALUES ('1',?,1)",a));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("INSERT INTO exam_venue VALUES (?,1)",b));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM student_venue_allocation WHERE exam_session_id=?",Integer.class,a));
    }
    @Test void manualOverlapIsRejectedWithoutChangingDraft() {
        int id=create("A","C");generate(id,1);
        int c=session(id,"C");
        var a=jdbc.queryForMap("SELECT * FROM exam_session WHERE exam_session_id=?",session(id,"A"));
        var before=service.detail(id);
        assertThrows(IllegalArgumentException.class,()->service.edit(id,c,new SchedulingRequests.Edit(2,((java.sql.Date)a.get("exam_date")).toLocalDate(),LocalTime.of(10,0),List.of(2))));
        assertEquals(before,service.detail(id));
    }
    @Test void successfulManualMoveReallocatesAndResetPreservesLegacyRecords() {
        int id=create("A");generate(id,1);
        service.edit(id,session(id,"A"),new SchedulingRequests.Edit(2,LocalDate.of(2090,1,3),LocalTime.of(11,0),List.of(2)));
        assertEquals(2,jdbc.queryForObject("SELECT venue_id FROM student_venue_allocation WHERE exam_session_id=?",Integer.class,session(id,"A")));
        service.reset(id,3);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM exam_session",Integer.class));
    }
    @Test void unavailableIntervalsAndCapacityChangesAreRespected() {
        int id=create("A");
        service.unavailable(1,new SchedulingRequests.Unavailability(LocalDateTime.of(2090,1,2,9,30),LocalDateTime.of(2090,1,2,12,0),"Maintenance"));
        generate(id,1);
        assertEquals(2,jdbc.queryForObject("SELECT venue_id FROM student_venue_allocation WHERE exam_session_id=?",Integer.class,session(id,"A")));
        assertThrows(IllegalStateException.class,()->service.unavailable(2,new SchedulingRequests.Unavailability(LocalDateTime.of(2090,1,2,10,0),LocalDateTime.of(2090,1,2,12,0),"Conflict")));
    }
    @Test void publicationDetectsInvigilatorOverlapsAndRegistrationChanges() {
        int id=create("A","B");generate(id,1);staffEveryBooking(id);
        jdbc.update("UPDATE invigilator_assignment SET staff_id=1 WHERE exam_session_id=?",session(id,"B"));
        assertTrue(service.validate(id).problems().stream().anyMatch(p->p.contains("overlapping duties")));
        assertThrows(IllegalStateException.class,()->service.publish(id,2));
        jdbc.update("DELETE FROM invigilator_assignment WHERE exam_session_id=?",session(id,"B"));
        jdbc.update("INSERT INTO student_registration VALUES ('3','A','2090/2091',1)");
        assertTrue(service.validate(id).problems().stream().anyMatch(p->p.contains("allocations differ")));
    }
    @Test void ordinaryAdministratorCanManageDraftWithoutCoordinatorAssignment() {
        int id=create("A");generate(id,1);
        assertFalse(service.detail(id).containsKey("coordinator_staff_id"));
        service.edit(id,session(id,"A"),new SchedulingRequests.Edit(2,LocalDate.of(2090,1,3),LocalTime.of(11,0),List.of(2)));
        generate(id,3);
        assertThrows(IllegalStateException.class,()->generate(id,2));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM exam_session WHERE period_id=?",Integer.class,id));
        staffEveryBooking(id);service.publish(id,4);
        assertEquals("PUBLISHED",service.detail(id).get("status"));
        assertFalse(jdbc.queryForObject("SELECT count(*)>0 FROM scheduling_audit WHERE period_id=? AND action='DENIED'",Boolean.class,id));
    }
    @Test void alternativesKeepOtherExamsFixedAndSavedSuggestionIsRevalidated() {
        int id=create("A","C");generate(id,1);
        var choices=service.alternatives(id,session(id,"A"));
        assertFalse(choices.isEmpty());
        var choice=choices.getFirst();
        for(int venue:choice.venues()) service.unavailable(venue,new SchedulingRequests.Unavailability(choice.start().plusDays(10),choice.end().plusDays(10),"Outside schedule"));
        var before=service.detail(id).get("exams");
        assertThrows(IllegalStateException.class,()->service.edit(id,session(id,"A"),new SchedulingRequests.Edit(1,choice.start().toLocalDate(),choice.start().toLocalTime(),choice.venues())));
        assertEquals(before,service.detail(id).get("exams"));
    }

    @Test void capacityDraftAndChangeRequestsNeverMutateTimetableOrCapacity() {
        int id=create("A","B");generate(id,1);staffEveryBooking(id);service.publish(id,2);
        jdbc.execute("ALTER TABLE staff ADD COLUMN full_name text, ADD COLUMN email text");
        jdbc.update("UPDATE staff SET full_name='Assigned lecturer',email='assigned@example.invalid' WHERE staff_id=2");
        jdbc.update("INSERT INTO course_lecturer VALUES ('A',2)");
        var review=context.getBean(SchedulingReviewService.class);
        var before=service.detail(id);
        var capacities=service.venues();
        var draft=review.capacityDraft(new SchedulingReviewService.CapacityDraft(id,"A",1,2));
        assertEquals("assigned@example.invalid",draft.get("to"));
        assertEquals("DRAFT_ONLY",draft.get("deliveryStatus"));
        assertEquals(false,draft.get("sendAvailable"));
        assertEquals(capacities,service.venues());assertEquals(before,service.detail(id));
        assertThrows(IllegalArgumentException.class,()->review.capacityDraft(new SchedulingReviewService.CapacityDraft(id,"B",1,2)));
        var actor=context.getBean(com.backend.fourth.common.security.CurrentStaffResolver.class).requireCurrentStaff();
        actor.setStaffId(2);var lecturer=new com.backend.fourth.staff.entity.Role();lecturer.setName("LECTURER");actor.setRoles(Set.of(lecturer));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->review.submit(new SchedulingReviewService.Change(id,"B",null,"Move","Reason")));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->review.submit(new SchedulingReviewService.Change(id,"A",session(id,"B"),"Move","Reason")));
        var request=review.submit(new SchedulingReviewService.Change(id,"A",session(id,"A"),"Please review venue","Verified correction requested"));
        long requestId=((Number)request.get("request_id")).longValue();
        assertEquals(1,review.list(id).size());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->review.decide(requestId,new SchedulingReviewService.Decision("APPROVED","Review")));
        actor.setStaffId(1);var admin=new com.backend.fourth.staff.entity.Role();admin.setName("ADMINISTRATOR");actor.setRoles(Set.of(admin));
        review.decide(requestId,new SchedulingReviewService.Decision("APPROVED","Proposal accepted for separately validated implementation"));
        assertEquals(before,service.detail(id));
        assertThrows(IllegalStateException.class,()->review.decide(requestId,new SchedulingReviewService.Decision("REJECTED","Stale decision")));
    }

    @Test void publishedTimeAmendmentRequiresApprovalRevalidationAuditAndIndependentNotifications() {
        int id=create("A");generate(id,1);staffEveryBooking(id);service.publish(id,2);
        int exam=session(id,"A");
        jdbc.update("INSERT INTO course_lecturer VALUES ('A',2)");
        var amendments=context.getBean(SchedulingAmendmentService.class);
        var notifications=context.getBean(SchedulingNotificationService.class);
        var allocations=service.detail(id).get("allocations");
        var date=LocalDate.of(2090,1,3);
        var proposal=amendments.propose(id,new SchedulingAmendmentService.Proposal(exam,3,date,LocalTime.of(11,0),"Room opening time corrected"));
        long amendment=((Number)proposal.get("amendment_id")).longValue();
        assertThrows(IllegalStateException.class,()->amendments.apply(id,amendment));
        amendments.decide(id,amendment,new SchedulingAmendmentService.Decision("APPROVED","Verified with faculty"));
        assertEquals(LocalDate.of(2090,1,2),jdbc.queryForObject("SELECT exam_date FROM exam_session WHERE exam_session_id=?",java.sql.Date.class,exam).toLocalDate());
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE exam_session SET exam_date=? WHERE exam_session_id=?",date,exam));
        int venue=jdbc.queryForObject("SELECT venue_id FROM exam_venue WHERE exam_session_id=?",Integer.class,exam);
        service.unavailable(venue,new SchedulingRequests.Unavailability(date.atTime(11,0),date.atTime(13,0),"New conflict after approval"));
        assertThrows(IllegalStateException.class,()->amendments.apply(id,amendment));
        assertEquals(3,revision(id));assertTrue(amendments.notifications(id,amendment).isEmpty());
        jdbc.update("DELETE FROM venue_unavailability WHERE reason='New conflict after approval'");
        var applied=amendments.apply(id,amendment);
        assertEquals("APPLIED",applied.get("status"));assertNotNull(applied.get("before_arrangement"));assertNotNull(applied.get("after_arrangement"));
        assertEquals(date,jdbc.queryForObject("SELECT exam_date FROM exam_session WHERE exam_session_id=?",java.sql.Date.class,exam).toLocalDate());
        assertEquals(allocations,service.detail(id).get("allocations"));assertEquals(4,revision(id));
        assertThrows(IllegalStateException.class,()->amendments.apply(id,amendment));
        assertEquals(3,amendments.notifications(id,amendment).size());
        assertTrue(amendments.notifications(id,amendment).stream().allMatch(n->"AVAILABLE".equals(n.get("delivery_status"))));
        jdbc.update("UPDATE examination_notification SET delivery_status='FAILED' WHERE amendment_id=?",amendment);
        amendments.retryNotifications(id,amendment);
        assertTrue(amendments.notifications(id,amendment).stream().allMatch(n->"AVAILABLE".equals(n.get("delivery_status"))));
        assertEquals(3,amendments.notifications(id,amendment).size());
        assertEquals(4,revision(id));
        var security=org.springframework.security.core.context.SecurityContextHolder.getContext();
        security.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("1","",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("STUDENT"))));
        try {
            assertEquals(1,notifications.inbox().size());
            long own=((Number)notifications.inbox().getFirst().get("notification_id")).longValue();notifications.read(own);
            security.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("unrelated","",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("STUDENT"))));
            assertTrue(notifications.inbox().isEmpty());
            assertThrows(org.springframework.security.access.AccessDeniedException.class,()->notifications.read(own));
        } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();}
        jdbc.update("UPDATE exam_session SET status='IN_PROGRESS' WHERE exam_session_id=?",exam);
        assertThrows(IllegalStateException.class,()->amendments.propose(id,new SchedulingAmendmentService.Proposal(exam,4,date,LocalTime.of(9,0),"Too late")));
    }

    @Test void concurrentGenerationUsesRevisionAndCommitsOnlyOnce() throws Exception {
        int id=create("A","B");
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Boolean> action=()->{try { generate(id,1);return true; } catch(IllegalStateException ex) {return false;}};
            var results=executor.invokeAll(List.of(action,action));
            assertEquals(1,results.stream().filter(f->{try{return f.get();}catch(Exception ex){throw new RuntimeException(ex);}}).count());
        }
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM exam_session WHERE period_id=?",Integer.class,id));
    }
    @Test void concurrentAllocationsCannotExceedExaminationCapacity() throws Exception {
        int id=create("A");generate(id,1);int exam=session(id,"A");
        jdbc.update("INSERT INTO student_registration VALUES ('3','A','2090/2091',1),('4','A','2090/2091',1)");
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Boolean>> actions=List.of(
                () -> insertExtra("3",exam), () -> insertExtra("4",exam));
            var results=executor.invokeAll(actions);
            assertEquals(1,results.stream().filter(f->{try{return f.get();}catch(Exception ex){throw new RuntimeException(ex);}}).count());
        }
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM student_venue_allocation WHERE exam_session_id=? AND venue_id=1",Integer.class,exam));
        assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("UPDATE venue SET examination_capacity=1 WHERE venue_id=1"));
    }
    private boolean insertExtra(String student,int exam) {
        try { jdbc.update("INSERT INTO student_venue_allocation VALUES (?,?,1)",student,exam);return true; }
        catch(org.springframework.dao.DataAccessException ex) {return false;}
    }

    @Test void inactiveInvigilatorsBlockPublicationEvenIfOtherStaffCoverTheRoom() {
        int id=create("A");generate(id,1);staffEveryBooking(id);
        jdbc.update("INSERT INTO invigilator_assignment(exam_session_id,venue_id,staff_id) VALUES (?,1,2)",session(id,"A"));
        jdbc.update("UPDATE staff SET account_status='SUSPENDED' WHERE staff_id=2");
        assertTrue(service.validate(id).problems().stream().anyMatch(p -> p.contains("not an active invigilator")));
        assertThrows(IllegalStateException.class,()->service.publish(id,2));
    }

}
