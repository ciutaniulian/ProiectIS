package ro.expensesplitter.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.service.ChatService;

@RestController
@RequestMapping("/api/group/{groupId}")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/messages")
    public Page<ChatMessageResponse> getMessages(@PathVariable Long groupId, @PageableDefault Pageable pageable){
        return chatService.getMessages(groupId,pageable);
    }
}
