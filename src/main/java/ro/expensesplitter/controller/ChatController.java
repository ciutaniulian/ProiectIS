package ro.expensesplitter.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.expensesplitter.dto.ChatHistoryResponse;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.service.ChatService;
import ro.expensesplitter.service.GroupChatAccess;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/group")
public class ChatController {

    private final ChatService chatService;
    private final GroupChatAccess chatAccess;

    public ChatController(ChatService chatService, GroupChatAccess chatAccess) {
        this.chatService = chatService;
        this.chatAccess = chatAccess;
    }

    @GetMapping(value = "/{groupId}/messages", params = "!limit")
    public Page<ChatMessageResponse> getMessages(@PathVariable Long groupId, Principal principal,
                                                 @PageableDefault Pageable pageable) {
        return chatService.getMessages(groupId, principal, pageable);
    }

    @GetMapping(value = "/{groupId}/messages", params = "limit")
    public ChatHistoryResponse getHistory(@PathVariable Long groupId, Principal principal,
                                          @RequestParam(required = false) Long before,
                                          @RequestParam(required = false) Long after,
                                          @RequestParam int limit) {
        return chatService.getHistory(groupId, principal, before, after, limit);
    }

    @GetMapping("/chat-memberships")
    public List<Long> getChatMemberships(Principal principal) {
        return chatAccess.groupIds(principal);
    }
}
