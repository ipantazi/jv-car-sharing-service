package com.github.ipantazi.carsharing.notification;

import static com.github.ipantazi.carsharing.util.TestDataUtil.ESCAPED_MESSAGE_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.TELEGRAM_BOT_TOKEN_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.TELEGRAM_CHAT_ID_TEST;
import static com.github.ipantazi.carsharing.util.TestDataUtil.TELEGRAM_MESSAGE_TEST;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.ipantazi.carsharing.notification.dto.TelegramMessageRequest;
import com.github.ipantazi.carsharing.notification.impl.TelegramClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class TelegramClientTest {
    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private TelegramClientImpl telegramClient;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        telegramClient = new TelegramClientImpl(
                TELEGRAM_BOT_TOKEN_TEST,
                TELEGRAM_CHAT_ID_TEST,
                webClientBuilder
        );
    }

    @Test
    @DisplayName("Should send message successfully")
    public void sendMessage_ValidText_SendMessageSuccessfully() {
        // Given
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any(TelegramMessageRequest.class)))
                .thenAnswer(invocation -> requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.empty());

        // When
        telegramClient.sendMessage(TELEGRAM_MESSAGE_TEST);

        // Then
        verify(webClient).post();
        verify(requestBodyUriSpec).uri(contains(TELEGRAM_BOT_TOKEN_TEST));
        verify(requestBodySpec).bodyValue(argThat(req -> {
            TelegramMessageRequest msg = (TelegramMessageRequest) req;
            return msg.chatId().equals(TELEGRAM_CHAT_ID_TEST)
                    && msg.text().equals(ESCAPED_MESSAGE_TEST);
        }));
        verify(requestHeadersSpec, times(1)).retrieve();
        verify(responseSpec, times(1)).toBodilessEntity();
    }

    @Test
    @DisplayName("Should not send message if text is null")
    public void sendMessage_NullText_NotSendMessage() {
        // When
        telegramClient.sendMessage(null);

        // Then
        verify(webClient, never()).post();
    }

    @Test
    @DisplayName("Should not send message if text is empty")
    public void sendMessage_EmptyText_NotSendMessage() {
        // When
        telegramClient.sendMessage("");

        // Then
        verify(webClient, never()).post();
    }

    @Test
    @DisplayName("Should not send message if text is blank")
    public void sendMessage_BlankText_NotSendMessage() {
        // When
        telegramClient.sendMessage("   ");

        // Then
        verify(webClient, never()).post();
    }

    @Test
    @DisplayName("Should not send message if telegram bot token is null")
    public void sendMessage_NullTelegramBotToken_ThrowException() {
        // When & Then
        assertThatThrownBy(() -> new TelegramClientImpl(
                null,
                TELEGRAM_CHAT_ID_TEST,
                webClientBuilder
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telegram bot token cannot be null or blank.");

        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("Should not send message if telegram chat id is null")
    public void sendMessage_NullTelegramChatId_ThrowException() {
        // When & Then
        assertThatThrownBy(() -> new TelegramClientImpl(
                TELEGRAM_BOT_TOKEN_TEST,
                null,
                webClientBuilder
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telegram chat id cannot be null or blank.");

        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("Should not send message if telegram bot token is blank")
    public void sendMessage_BlankTelegramBotToken_ThrowException() {
        // When & Then
        assertThatThrownBy(() -> new TelegramClientImpl(
                "   ",
                TELEGRAM_CHAT_ID_TEST,
                webClientBuilder
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telegram bot token cannot be null or blank.");

        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("Should not send message if telegram chat id is blank")
    public void sendMessage_BlankTelegramChatId_ThrowException() {
        // When & Then
        assertThatThrownBy(() -> new TelegramClientImpl(
                TELEGRAM_BOT_TOKEN_TEST,
                "   ",
                webClientBuilder
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Telegram chat id cannot be null or blank.");

        verifyNoInteractions(webClient);
    }
}
