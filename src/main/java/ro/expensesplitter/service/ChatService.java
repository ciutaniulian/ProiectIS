package ro.expensesplitter.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ro.expensesplitter.dto.ChatHistoryResponse;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.dto.SendMessageRequest;
import ro.expensesplitter.entity.GroupMessage;
import ro.expensesplitter.entity.GroupStatus;
import ro.expensesplitter.repository.ExpenseGroupRepository;
import ro.expensesplitter.repository.GroupMessageRepository;
import ro.expensesplitter.repository.UserRepository;

import java.security.Principal;
import java.util.List;

@Service
public class ChatService {
    private final GroupMessageRepository messages;
    private final UserRepository users;
    private final GroupChatAccess access;
    private final ExpenseGroupRepository groups;

    public ChatService(GroupMessageRepository messages, UserRepository users, GroupChatAccess access,
                       ExpenseGroupRepository groups) {
        this.messages = messages;
        this.users = users;
        this.access = access;
        this.groups = groups;
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long groupId, Principal principal, SendMessageRequest request) {
        var group = groups.findForUpdate(groupId).orElseThrow(() -> new AccessDeniedException("Group membership required"));
        access.requireMember(groupId, principal);
        if (group.getStatus() != GroupStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group is archived");
        }
        var sender = users.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        String requestId = request.requestId() == null ? null : request.requestId().toString();
        if (requestId != null) {
            var existing = messages.findByGroupIdAndSenderIdAndClientMessageId(groupId, sender.getId(), requestId);
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }
        return toResponse(messages.save(new GroupMessage(groupId, sender, request.message(), requestId)));
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(Long groupId, Principal principal, Pageable page) {
        access.requireMember(groupId, principal);
        return messages.findByGroupIdOrderByCreatedAtDesc(groupId, page).map(ChatService::toResponse);
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getHistory(Long groupId, Principal principal, Long before, Long after, int limit) {
        access.requireMember(groupId, principal);
        if (limit < 1 || limit > 100 || (before != null && (before < 1 || after != null))
                || (after != null && after < 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid chat cursor");
        }
        var page = PageRequest.of(0, limit + 1);
        List<GroupMessage> result;
        if (after != null) {
            result = messages.findByGroupIdAndIdGreaterThanOrderByIdAsc(groupId, after, page);
        } else if (before != null) {
            result = messages.findByGroupIdAndIdLessThanOrderByIdDesc(groupId, before, page);
        } else {
            result = messages.findByGroupIdOrderByIdDesc(groupId, page);
        }
        return new ChatHistoryResponse(result.stream().limit(limit).map(ChatService::toResponse).toList(),
                result.size() > limit);
    }

    @Transactional(readOnly = true)
    public GroupStatus status(Long groupId, Principal principal) {
        access.requireMember(groupId, principal);
        return groups.findById(groupId).orElseThrow(() -> new AccessDeniedException("Group membership required"))
                .getStatus();
    }

    private static ChatMessageResponse toResponse(GroupMessage message) {
        var sender = message.getSender();
        return new ChatMessageResponse(message.getId(), message.getGroupId(), sender.getId(),
                sender.getName(), message.getMessage(), message.getCreatedAt());
    }
}
