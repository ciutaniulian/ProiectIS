package ro.expensesplitter.controller;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.service.ChatService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatControllerTest {

    @Test
    void historyUsesTheAuthenticatedUser() {
        ChatService chatService = mock(ChatService.class);
        ChatController controller = new ChatController(chatService);
        var pageable = PageRequest.of(0, 20);
        var page = new PageImpl<>(List.of(mock(ChatMessageResponse.class)));
        when(chatService.getMessages(1L, "ana@example.com", pageable)).thenReturn(page);

        assertEquals(page, controller.getMessages(1L, pageable, () -> "ana@example.com"));
    }
}
