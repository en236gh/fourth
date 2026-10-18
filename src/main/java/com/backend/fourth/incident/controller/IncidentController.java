package com.backend.fourth.incident.controller;

import com.backend.fourth.common.ApiResponse;
import com.backend.fourth.common.security.CurrentStaffResolver;
import com.backend.fourth.incident.dto.CreateIncidentRequest;
import com.backend.fourth.incident.dto.EvidenceDownloadUrlResponse;
import com.backend.fourth.incident.dto.IncidentEvidenceResponse;
import com.backend.fourth.incident.dto.IncidentResponse;
import com.backend.fourth.incident.service.IncidentService;
import com.backend.fourth.staff.entity.Staff;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;
    private final CurrentStaffResolver currentStaffResolver;

    @PostMapping
    @PreAuthorize("hasAuthority('INVIGILATOR')")
    public ApiResponse<IncidentResponse> report(@Valid @RequestBody CreateIncidentRequest request) {
        Staff staff = currentStaffResolver.requireCurrentStaff();
        return ApiResponse.success("Incident reported", incidentService.report(request, staff));
    }

    @PostMapping(value = "/{incidentId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('INVIGILATOR') or hasAuthority('ADMINISTRATOR')")
    public ApiResponse<IncidentEvidenceResponse> uploadEvidence(
            @PathVariable Integer incidentId,
            @RequestPart("file") MultipartFile file) {
        Staff staff = currentStaffResolver.requireCurrentStaff();
        return ApiResponse.success("Evidence uploaded", incidentService.uploadEvidence(incidentId, file, staff));
    }

    @GetMapping("/{incidentId}/evidence")
    @PreAuthorize("hasAuthority('INVIGILATOR') or hasAuthority('ADMINISTRATOR')")
    public ApiResponse<List<IncidentEvidenceResponse>> listEvidence(@PathVariable Integer incidentId) {
        Staff staff = currentStaffResolver.requireCurrentStaff();
        return ApiResponse.success("Evidence retrieved", incidentService.listEvidence(incidentId, staff));
    }

    @GetMapping("/{incidentId}/evidence/{evidenceId}/download-url")
    @PreAuthorize("hasAuthority('INVIGILATOR') or hasAuthority('ADMINISTRATOR')")
    public ApiResponse<EvidenceDownloadUrlResponse> evidenceDownloadUrl(
            @PathVariable Integer incidentId,
            @PathVariable Integer evidenceId) {
        Staff staff = currentStaffResolver.requireCurrentStaff();
        return ApiResponse.success("Download URL generated",
                incidentService.createEvidenceDownloadUrl(incidentId, evidenceId, staff));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVIGILATOR') or hasAuthority('ADMINISTRATOR')")
    public ApiResponse<List<IncidentResponse>> list() {
        Staff staff = currentStaffResolver.requireCurrentStaff();
        boolean isAdmin = staff.getRoles().stream()
                .anyMatch(role -> "ADMINISTRATOR".equals(role.getName()));
        List<IncidentResponse> incidents = isAdmin
                ? incidentService.listForAdmin()
                : incidentService.listForInvigilator(staff);
        return ApiResponse.success("Incidents retrieved", incidents);
    }
}
