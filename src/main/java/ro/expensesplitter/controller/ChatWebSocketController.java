package ro.expensesplitter.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;
import ro.expensesplitter.dto.ChatReply;
import ro.expensesplitter.dto.SendMessageRequest;
import ro.expensesplitter.entity.GroupStatus;
import ro.expensesplitter.service.ChatService;
import ro.expensesplitter.service.GroupStatusChanged;

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
    @SendToUser(value = "/queue/chat/replies", broadcast = false)
    public ChatReply sendMessage(@DestinationVariable Long groupId, @Payload @Valid SendMessageRequest request,
                                 Principal principal) {
        try {
            var response = chatService.sendMessage(groupId, principal, request);
            template.convertAndSend("/topic/group/" + groupId, response);
            return new ChatReply(groupId, request.requestId(), ChatReply.Result.SENT, null, response);
        } catch (AccessDeniedException denied) {
            return new ChatReply(groupId, request.requestId(), ChatReply.Result.ACCESS_LOST, null, null);
        } catch (ResponseStatusException failure) {
            if (failure.getStatusCode() == HttpStatus.CONFLICT) {
                return new ChatReply(groupId, request.requestId(), ChatReply.Result.ARCHIVED, GroupStatus.ARCHIVED, null);
            }
            throw failure;
        }
    }

    @MessageMapping("/chat/{groupId}/ready")
    @SendToUser(value = "/queue/chat/replies", broadcast = false)
    public ChatReply ready(@DestinationVariable Long groupId, Principal principal) {
        try {
            return new ChatReply(groupId, null, ChatReply.Result.READY, chatService.status(groupId, principal), null);
        } catch (AccessDeniedException denied) {
            return new ChatReply(groupId, null, ChatReply.Result.ACCESS_LOST, null, null);
        }
    }

    @MessageExceptionHandler({MessagingException.class, ResponseStatusException.class})
    @SendToUser(value = "/queue/chat/replies", broadcast = false)
    public ChatReply requestFailed() {
        return new ChatReply(null, null, ChatReply.Result.FAILED, null, null);
    }

    @TransactionalEventListener
    public void notifyStateChanged(GroupStatusChanged event) {
        template.convertAndSend("/topic/group/" + event.groupId(), event);
    }
}
