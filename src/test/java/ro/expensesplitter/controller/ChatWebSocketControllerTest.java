package ro.expensesplitter.controller;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.dto.SendMessageRequest;
import ro.expensesplitter.service.ChatService;

import java.time.LocalDateTime;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatWebSocketControllerTest {

    @Test
    void persistsBeforeDelivering() {
        ChatService chatService = mock(ChatService.class);
        ChatWebSocketController controller = new ChatWebSocketController(chatService);
        var request = new SendMessageRequest("Hello");
        var response = new ChatMessageResponse(1L, 2L, 3L, "Ana", "Hello", LocalDateTime.now());
        when(chatService.sendMessage(2L, "ana@example.com", request)).thenReturn(response);

        controller.sendMessage(2L, request, () -> "ana@example.com");

        InOrder order = inOrder(chatService);
        order.verify(chatService).sendMessage(2L, "ana@example.com", request);
        order.verify(chatService).deliverMessage(response);
    }
}
