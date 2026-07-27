package group1.com.MangaSystemAndManagement.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({EntityNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, WebRequest request) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex, WebRequest request) {
        return response(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", ex.getMessage(), request, null);
    }

    @ExceptionHandler({DuplicateEntityException.class, AccountStateConflictException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex, WebRequest request) {
        return response(HttpStatus.CONFLICT, "INVALID_ACCOUNT_STATE", ex.getMessage(), request, null);
    }

    /**
     * BA V3 §3.2: optimistic locking — if a Chapter was updated by another user between
     * read and write, surface as HTTP 409 with a clear hint to reload the page.
     */
    @ExceptionHandler({OptimisticLockingFailureException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLock(RuntimeException ex, WebRequest request) {
        return response(HttpStatus.CONFLICT, "STALE_ENTITY",
                "Trạng thái Chapter đã được cập nhật bởi người dùng khác. Vui lòng tải lại trang.",
                request, null);
    }

    @ExceptionHandler({InvalidPublicRoleException.class, IllegalArgumentException.class,
            ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, WebRequest request) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request, null);
    }

    /**
     * Workflow-rule violations are an HTTP 400 – the request was well-formed
     * but it would break a business invariant (e.g. creating a SubTask whose
     * deadline exceeds the parent Task's deadline, or submitting a polymorphic
     * submission without disambiguating taskId/subTaskId).
     *
     * <p>Without this handler the exception falls through to the generic
     * {@code Exception} handler below and surfaces as a 500 with the unhelpful
     * "An unexpected error occurred" body – which is exactly what callers were
     * seeing when they submitted malformed submissions via Swagger.</p>
     */
    @ExceptionHandler(WorkflowRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleWorkflowRuleViolation(WorkflowRuleViolationException ex,
                                                                    WebRequest request) {
        return response(HttpStatus.BAD_REQUEST, "WORKFLOW_RULE_VIOLATION", ex.getMessage(), request, null);
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(RuntimeException ex, WebRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password", request, null);
    }

    @ExceptionHandler(AccountStatusException.class)
    public ResponseEntity<ErrorResponse> handleAccountStatus(AccountStatusException ex, WebRequest request) {
        Map<String, Object> details = ex.getRejectionReason() == null
                ? null
                : Map.of("rejectionReason", ex.getRejectionReason());
        return response(HttpStatus.FORBIDDEN, ex.getErrorCode(), ex.getMessage(), request, details);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return response(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, WebRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication required", request, null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ErrorResponse body = body(
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, details);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            String errorCode,
            String message,
            WebRequest request,
            Map<String, Object> details) {
        return new ResponseEntity<>(body(status, errorCode, message, request, details), status);
    }

    private ErrorResponse body(
            HttpStatus status,
            String errorCode,
            String message,
            WebRequest request,
            Map<String, Object> details) {
        ErrorResponse body = new ErrorResponse(status.value(), message, status.getReasonPhrase());
        body.setPath(request.getDescription(false).replace("uri=", ""));
        body.setErrorCode(errorCode);
        body.setDetails(details);
        return body;
    }
}
