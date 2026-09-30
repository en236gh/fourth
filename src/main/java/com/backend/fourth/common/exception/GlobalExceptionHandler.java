package com.backend.fourth.common.exception;

import com.backend.fourth.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.stream.Collectors;

@RestControllerAdvice
@lombok.RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final com.backend.fourth.scheduling.SchedulingDeniedAudit schedulingAudit;
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException ex, jakarta.servlet.http.HttpServletRequest request) {
        String path=request.getRequestURI();
        if (!java.util.Set.of("GET","HEAD","OPTIONS").contains(request.getMethod()) &&
                (path.startsWith("/api/admin/examination-periods") || path.startsWith("/api/admin/invigilator-assignments") || path.startsWith("/api/examination-change-requests"))) {
            var authentication=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            try { schedulingAudit.recordHttp(authentication==null?null:authentication.getName(),request.getMethod()+" "+path); }
            catch(RuntimeException unavailable) { log.warn("Denied scheduling mutation could not be persisted to the audit store"); }
        }
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMalformedRequest(Exception ex) {
        return ApiResponse.error("Invalid request format or missing parameter. Check the documented field names, dates, times and numeric values.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleConflict(IllegalStateException ex) {
        return ApiResponse.error(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Validation failed";
        }
        return ApiResponse.error(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = "Validation failed";
        }
        return ApiResponse.error(message);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        return ApiResponse.error("Change conflicts with database integrity rules. Refresh the examination and review registrations, bookings, capacity and staffing.");
    }

    @ExceptionHandler({org.springframework.dao.DataAccessResourceFailureException.class,
            org.springframework.transaction.TransactionException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResponse<Void> handleOperationalFailure(RuntimeException ex) {
        return ApiResponse.error("Database operation could not be confirmed. Check the current timetable status and revision before explicitly retrying. Do not assume the operation failed.");
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(RuntimeException ex) {
        return ApiResponse.error("An unexpected operational failure occurred. Check status before retrying.");
    }
}
