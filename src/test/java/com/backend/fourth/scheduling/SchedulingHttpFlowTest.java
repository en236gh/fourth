package com.backend.fourth.scheduling;

import com.backend.fourth.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/** Requires the disposable full-schema database and phase_24 seed; never runs by default. */
@EnabledIfSystemProperty(named="scheduling.http",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class SchedulingHttpFlowTest {
    @Value("${local.server.port}") int port;
    @Autowired JwtService jwt;
    @Autowired JdbcTemplate jdbc;
    private final ObjectMapper mapper=new ObjectMapper();
    private String admin;
    private JsonNode request(String method,String path,String body,String token,int status) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Authorization","Bearer "+token).header("Content-Type","application/json");
        builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        var response=HttpClient.newHttpClient().send(builder.build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(status,response.statusCode(),response.body());
        return mapper.readTree(response.body());
    }
    @Test void administratorToStudentAndLecturerPublicationFlow() throws Exception {
        try(var c=jdbc.getDataSource().getConnection()) {
            assertTrue(c.getMetaData().getURL().matches("jdbc:postgresql://127\\.0\\.0\\.1:55439/scheduling_http_[a-f0-9]{32}"),"Only run against a uniquely named disposable HTTP database");
        }
        jdbc.update("INSERT INTO staff(full_name,email,account_status) VALUES ('Local Test Admin','scheduling-admin@example.invalid','ACTIVE') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO staff_role SELECT s.staff_id,r.role_id FROM staff s CROSS JOIN role r WHERE s.email='scheduling-admin@example.invalid' AND r.name='ADMINISTRATOR' ON CONFLICT DO NOTHING");
        admin=jwt.generateAccessToken("scheduling-admin@example.invalid",List.of("ADMINISTRATOR"));
        String lecturer=jwt.generateAccessToken("scheduling-lecturer@example.invalid",List.of("LECTURER"));
        String student=jwt.generateAccessToken("SD000001",List.of("STUDENT"));
        request("GET","/api/admin/examination-periods",null,lecturer,403);
        request("GET","/api/admin/examination-periods",null,student,403);
        LocalDate start=jdbc.queryForObject("SELECT exam_date FROM exam_session WHERE course_code='DEMOEXT'",java.sql.Date.class).toLocalDate();
        String year=jdbc.queryForObject("SELECT academic_year FROM exam_session WHERE course_code='DEMOEXT'",String.class);
        String root="/api/admin/examination-periods";
        var created=request("POST",root,"""
                {"name":"HTTP demonstration","semester":1,"examType":"FINAL",
                "startDate":"%s","endDate":"%s","daysOfWeek":[1,2,3,4,5],
                "slots":[{"startTime":"09:00","endTime":"11:00"},{"startTime":"11:00","endTime":"13:00"}]}
                """.formatted(start,start.plusDays(4)),admin,200);
        assertEquals(year,created.path("data").path("academic_year").asText());
        int id=created.path("data").path("period_id").asInt();assertTrue(id>0);
        String period=root+"/"+id;
        request("PUT",period+"/courses","""
                {"revision":0,"courses":[{"courseCode":"DEMO101","durationMinutes":120},
                {"courseCode":"DEMO102","durationMinutes":120},{"courseCode":"DEMO201","durationMinutes":120},
                {"courseCode":"DEMO301","durationMinutes":120}]}
                """,admin,200);
        var generated=request("POST",period+"/generate","{\"revision\":1,\"searchLimit\":100000}",admin,200);
        assertEquals("COMPLETE",generated.path("data").path("result").path("outcome").asText());
        int session=jdbc.queryForObject("SELECT exam_session_id FROM exam_session WHERE period_id=? AND course_code='DEMO101'",Integer.class,id);
        assertEquals(0,request("GET","/api/student/examinations",null,student,200).path("data").size());
        request("GET","/api/allocation/exam-session/"+session,null,lecturer,403);
        request("POST",period+"/publish","{\"revision\":2}",admin,409);
        request("POST",period+"/auto-assign-invigilators","{}",admin,200);
        assertTrue(request("GET",period+"/validation",null,admin,200).path("data").path("valid").asBoolean());
        request("POST",period+"/publish","{\"revision\":2}",admin,200);
        assertEquals(3,request("GET","/api/student/examinations",null,student,200).path("data").size());
        var counts=request("GET","/api/allocation/exam-session/"+session,null,lecturer,200).path("data");
        assertEquals(1,counts.path("registeredStudents").asInt());assertEquals(1,counts.path("allocatedStudents").asInt());assertEquals(0,counts.path("unallocatedStudents").asInt());
        var assignment=jdbc.queryForMap("SELECT a.venue_id,s.email FROM invigilator_assignment a JOIN staff s USING(staff_id) WHERE exam_session_id=? LIMIT 1",session);
        String invigilator=jwt.generateAccessToken((String)assignment.get("email"),List.of("INVIGILATOR"));
        assertEquals(1,request("GET","/api/invigilator/assignments/"+session+"/"+assignment.get("venue_id")+"/students",null,invigilator,200).path("data").size());
        request("POST",period+"/generate","{\"revision\":3,\"searchLimit\":100000}",admin,409);
    }
}
