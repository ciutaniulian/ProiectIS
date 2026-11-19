package ro.expensesplitter.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.dto.SendMessageRequest;
import ro.expensesplitter.service.ChatService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/{groupId}")
    public void sendMessage(@DestinationVariable Long groupId, @Payload @Valid SendMessageRequest request, Principal principal) {
        ChatMessageResponse message = chatService.sendMessage(groupId, principal.getName(), request);
        chatService.deliverMessage(message);
    }
}
