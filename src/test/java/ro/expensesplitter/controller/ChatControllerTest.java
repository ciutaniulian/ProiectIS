package ro.expensesplitter.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.service.ChatService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class ChatControllerTest {

    private ChatService chatService;

    private ChatMessageResponse mockResponse;

    @BeforeEach
    void setup()
    {
        chatService=mock(ChatService.class);
        mockResponse=mock(ChatMessageResponse.class);
    }

    @Test
    void returnedMessagesPageTest()
    {
        ChatController controller = new ChatController(chatService);

        Long groupId = 1L;
        Pageable pageable = PageRequest.of(0,20);

        Page<ChatMessageResponse> responses = new PageImpl<>(List.of(mockResponse));
        when(chatService.getMessages(groupId,pageable)).thenReturn(responses);

        Page<ChatMessageResponse> result = controller.getMessages(groupId,pageable);


        assertNotNull(result);
        assertEquals(responses,result);
        verify(chatService).getMessages(groupId,pageable);
    }
}
