package ro.expensesplitter.controller;

import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.dto.SendMessageRequest;
import ro.expensesplitter.service.ChatService;

import java.security.Principal;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;

    private final SimpMessagingTemplate template;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate template) {
        this.chatService = chatService;
        this.template = template;
    }

    @MessageMapping("/chat/{groupId}")
    public void sendMessage(@DestinationVariable Long groupId, @Payload @Valid SendMessageRequest request, Principal principal) {
        ChatMessageResponse response = chatService.sendMessage(groupId, principal.getName(), request);

        template.convertAndSend("/topic/group/" + groupId, response);
    }
}
