package ro.expensesplitter.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.dto.SendMessageRequest;
import ro.expensesplitter.service.ChatService;

import java.security.Principal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

public class ChatWebSocketControllerTest {

    private ChatService chatService;

    private SimpMessagingTemplate template;

    private ChatWebSocketController controller;

    @BeforeEach
    void setup()
    {
        chatService=mock(ChatService.class);
        template=mock(SimpMessagingTemplate.class);
        controller = new ChatWebSocketController(chatService,template);
    }

    @Test
    void sendMessageTest()
    {
        Long groupId = 1L;
        SendMessageRequest request = new SendMessageRequest("test");
        ChatMessageResponse response = new ChatMessageResponse(1L, groupId, 1L, "Ana", "test", LocalDateTime.of(2026, 1, 1, 12, 0));
        Principal principal = mock(Principal.class);

        when(principal.getName()).thenReturn("ana@test.com");

        when(chatService.sendMessage(groupId,"ana@test.com",request)).thenReturn(response);

        when(chatService.sendMessage(groupId, 1L, request)).thenReturn(response);

        controller.sendMessage(groupId,request,principal);

        verify(chatService).sendMessage(groupId, "ana@test.com", request);
        verify(template).convertAndSend("/topic/group/" + groupId, response);
    }

}
