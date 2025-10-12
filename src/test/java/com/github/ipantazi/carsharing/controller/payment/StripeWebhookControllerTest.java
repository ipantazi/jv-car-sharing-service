package com.github.ipantazi.carsharing.controller.payment;

import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_PAYLOAD_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.INVALID_SIG_HEADER_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.PAYLOAD_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.SIG_HEADER_TEST;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.BAD_REQUEST;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.INTERNAL_SERVER_ERROR;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.OK;
import static com.github.ipantazi.carsharing.util.controller.ControllerTestDataUtil.URL_WEBHOOK;
import static com.github.ipantazi.carsharing.util.controller.MvcTestHelper.createWebhookMvcResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.ipantazi.carsharing.controller.StripeWebhookController;
import com.github.ipantazi.carsharing.exception.InvalidStripePayloadException;
import com.github.ipantazi.carsharing.security.JwtUtil;
import com.github.ipantazi.carsharing.service.payment.stripe.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(StripeWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
public class StripeWebhookControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StripeWebhookService stripeWebhookService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("Test handleStripeEvent method with valid payload and signature")
    void handleStripeEvent_WhenServiceSucceeds_ReturnsOk() throws Exception {
        // When
        MvcResult result = createWebhookMvcResult(
                mockMvc,
                post(URL_WEBHOOK),
                status().isOk(),
                PAYLOAD_TEST,
                SIG_HEADER_TEST,
                "Webhook received"
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(OK);
    }

    @Test
    void postWebhook_WhenSignatureInvalid_ReturnsBadRequest() throws Exception {
        // Given
        doThrow(new SignatureVerificationException("Invalid signature", INVALID_SIG_HEADER_TEST))
                .when(stripeWebhookService).processStripeEvent(anyString(), anyString());

        // When
        MvcResult result = createWebhookMvcResult(
                mockMvc,
                post(URL_WEBHOOK),
                status().isBadRequest(),
                PAYLOAD_TEST,
                INVALID_SIG_HEADER_TEST,
                "Invalid signature"
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void postWebhook_WhenPayloadInvalid_ReturnsBadRequest() throws Exception {
        // Given
        doThrow(new InvalidStripePayloadException("Invalid payload"))
                .when(stripeWebhookService).processStripeEvent(anyString(), anyString());

        // When
        MvcResult result = createWebhookMvcResult(
                mockMvc,
                post(URL_WEBHOOK),
                status().isBadRequest(),
                INVALID_PAYLOAD_TEST,
                SIG_HEADER_TEST,
                "Invalid payload"
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void postWebhook_WhenUnexpectedException_ReturnsInternalServerError() throws Exception {
        // Given
        doThrow(new RuntimeException("Unexpected error"))
                .when(stripeWebhookService).processStripeEvent(anyString(), anyString());

        // When
        MvcResult result = createWebhookMvcResult(
                mockMvc,
                post(URL_WEBHOOK),
                status().isInternalServerError(),
                PAYLOAD_TEST,
                SIG_HEADER_TEST,
                "Error processing webhook"
        );

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
    }
}
