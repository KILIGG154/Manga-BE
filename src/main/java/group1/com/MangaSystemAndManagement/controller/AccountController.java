package group1.com.MangaSystemAndManagement.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import group1.com.MangaSystemAndManagement.dto.request.AccountLoginRequest;
import group1.com.MangaSystemAndManagement.dto.request.AccountRequest;
import group1.com.MangaSystemAndManagement.dto.request.RejectAccountRequest;
import group1.com.MangaSystemAndManagement.dto.request.SendOtpRequest;
import group1.com.MangaSystemAndManagement.dto.response.AccountResponse;
import group1.com.MangaSystemAndManagement.dto.response.ResponseBase;
import group1.com.MangaSystemAndManagement.model.Account;
import group1.com.MangaSystemAndManagement.service.interfaces.AccountService;
import group1.com.MangaSystemAndManagement.service.interfaces.OtpService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@Tag(name = "Accounts", description = "Account management APIs")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final OtpService otpService;

    @PostMapping("/auth/accounts")
    public ResponseEntity<ResponseBase> createAccount(@Valid @RequestBody AccountRequest request) {
        Map<String, Object> result = accountService.createAccount(request);
        return ResponseEntity.status(201)
                .body(new ResponseBase(201, "Registration request submitted", result));
    }

    @PostMapping("/auth/send-otp")
    public ResponseEntity<ResponseBase> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        otpService.generateAndSendOtp(request.getEmail());
        return ResponseEntity.ok(new ResponseBase(200, "OTP sent successfully", null));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ResponseBase> loginAccount(@RequestBody AccountLoginRequest loginRequest) {
        return ResponseEntity.ok(new ResponseBase(200, "Login successful", accountService.login(loginRequest)));
    }

    @GetMapping("/account-requests")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ResponseEntity<ResponseBase> getAccountRequests(
            @RequestParam(required = false) String status, Authentication authentication) {
        List<AccountResponse> requests = accountService.getAccountRequests(authentication.getName(), status);
        return ResponseEntity.ok(new ResponseBase(200, "Account requests retrieved", requests));
    }

    @PostMapping("/account-requests/{accountId}/approve")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ResponseEntity<ResponseBase> approveAccount(
            @PathVariable Long accountId, Authentication authentication) {
        AccountResponse account = accountService.approveAccountRole(accountId, authentication.getName());
        return ResponseEntity.ok(new ResponseBase(200, "Account request approved", account));
    }

    @PostMapping("/account-requests/{accountId}/reject")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ResponseEntity<ResponseBase> rejectAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody RejectAccountRequest request,
            Authentication authentication) {
        AccountResponse account = accountService.rejectAccountRole(
                accountId, authentication.getName(), request.getReason());
        return ResponseEntity.ok(new ResponseBase(200, "Account request rejected", account));
    }

    /**
     * Compatibility endpoint. roleName is intentionally ignored; the assigned role is always
     * derived from the account's stored requestedRole.
     */
    @PostMapping("/admin/accounts/{accountId}/approve")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ResponseEntity<ResponseBase> approveAccountRole(
            @PathVariable Long accountId,
            @RequestParam(required = false) String roleName,
            Authentication authentication) {
        AccountResponse account = accountService.approveAccountRole(accountId, authentication.getName());
        return ResponseEntity.ok(new ResponseBase(200, "Account request approved", account));
    }

    @GetMapping("/accounts")
//    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ResponseBase> getAllAccounts() {
        List<Account> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(new ResponseBase(200, "Accounts retrieved", accounts));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<ResponseBase> getAccountById(@PathVariable Long accountId) {
        Account account = accountService.getAccountById(accountId);
        return ResponseEntity.ok(new ResponseBase(200, "Account retrieved", account));
    }

    @PostMapping("/admin/accounts/{accountId}/deactivate")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ResponseEntity<ResponseBase> deactivateAccount(@PathVariable Long accountId) {
        accountService.deactivateAccount(accountId);
        return ResponseEntity.ok(new ResponseBase(200, "Account deactivated", null));
    }

    @PostMapping("/admin/accounts/{accountId}/activate")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('MANAGER')")
    public ResponseEntity<ResponseBase> activateAccount(@PathVariable Long accountId) {
        accountService.activateAccount(accountId);
        return ResponseEntity.ok(new ResponseBase(200, "Account activated", null));
    }

    @GetMapping("/accounts/search")
    public ResponseEntity<ResponseBase> searchAccountByEmail(@RequestParam String email) {
        group1.com.MangaSystemAndManagement.dto.response.AccountSearchResponse account = accountService.searchByEmail(email);
        return ResponseEntity.ok(new ResponseBase(200, "Account retrieved", account));
    }
}
