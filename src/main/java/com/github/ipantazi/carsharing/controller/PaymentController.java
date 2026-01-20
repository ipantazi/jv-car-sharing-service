package com.github.ipantazi.carsharing.controller;

import com.github.ipantazi.carsharing.dto.payment.PaymentRequestDto;
import com.github.ipantazi.carsharing.dto.payment.PaymentResponseDto;
import com.github.ipantazi.carsharing.model.User;
import com.github.ipantazi.carsharing.service.payment.PaymentService;
import com.github.ipantazi.carsharing.service.user.UserService;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@Tag(name = "Payment management", description = "Endpoints of managing payments")
@Validated
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final UserService userService;
    private final PaymentService paymentService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    @Operation(summary = "Get all payments", description = "Returns a list of all payments")
    public Page<PaymentResponseDto> getPayments(
            Authentication authentication,
            @RequestParam(value = "user_id", required = false)
            @Positive(message = "User ID must be a positive number")
            Long userId,
            @ParameterObject Pageable pageable
    ) {
        User user = (User) authentication.getPrincipal();
        Long actualUserId = userService.resolveUserIdForAccess(user, userId);
        return paymentService.getPayments(actualUserId, pageable);
    }

    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping
    @Operation(summary = "Create payment session", description = "Creates a new payment session")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponseDto createPayment(
            Authentication authentication,
            @RequestBody @Valid PaymentRequestDto paymentRequestDto,
            UriComponentsBuilder uriBuilder
    ) throws StripeException {

        User user = (User) authentication.getPrincipal();
        return paymentService.createPaymentSession(user.getId(), paymentRequestDto, uriBuilder);
    }

    @GetMapping("/success")
    @Operation(
            summary = "Handle payment success", description = "Handles the success of a payment"
    )
    public String handleSuccess(
            @NotBlank(message = "Session ID cannot be blank")
            @RequestParam("session_id")
            String sessionId
    ) {
        return paymentService.getPaymentSuccessMessage(sessionId);
    }

    @GetMapping("/cancel")
    @Operation(summary = "Handle payment cancel", description = "Handles the cancel of a payment")
    public String handleCancel() {
        return "Payment canceled. You can pay later, but the session is only valid for 24 hours.";
    }

    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PostMapping("/renew")
    @Operation(summary = "Renew payment session", description = "Renews a payment session")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponseDto renewPaymentSession(
            Authentication authentication,
            @RequestBody @Valid PaymentRequestDto paymentRequestDto,
            UriComponentsBuilder uriBuilder
    ) throws StripeException {
        User user = (User) authentication.getPrincipal();
        return paymentService.renewPaymentSession(user.getId(), paymentRequestDto, uriBuilder);
    }
}
