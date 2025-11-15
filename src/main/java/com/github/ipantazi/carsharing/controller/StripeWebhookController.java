package com.github.ipantazi.carsharing.controller;

import com.github.ipantazi.carsharing.exception.InvalidStripePayloadException;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Stripe Webhook management", description = "Endpoints for Stripe webhook management")
@RestController
@RequestMapping("/webhook/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {
    private final StripeWebhookService stripeWebhookService;

    @PostMapping
    @Operation(
            summary = "Stripe Webhook Handler",
            description = "Handles Stripe events (internal use only). Not for public consumption."
    )
    public ResponseEntity<String> handleStripeEvent(
            @RequestBody
            @NotBlank(message = "Payload cannot be empty")
            String payload,

            @RequestHeader("Stripe-Signature")
            @NotBlank(message = "Signature header cannot be empty")
            String sigHeader
    ) {
        try {
            stripeWebhookService.processStripeEvent(payload, sigHeader);
            return ResponseEntity.ok("Webhook received");
        } catch (SignatureVerificationException e) {
            log.warn("❌ Invalid Stripe Signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (InvalidStripePayloadException e) {
            log.warn("❌ Invalid Stripe Payload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        } catch (Exception e) {
            log.error("❌ Unexpected error while processing Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook");
        }
    }
}
