package ro.expensesplitter.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.dto.SendMessageRequest;
import ro.expensesplitter.entity.GroupMessage;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.GroupMemberRepository;
import ro.expensesplitter.repository.GroupMessageRepository;
import ro.expensesplitter.repository.GroupRepository;
import ro.expensesplitter.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final GroupMessageRepository groupMessageRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatMessageResponse sendMessage(Long groupId, String email, SendMessageRequest request) {
        lockGroup(groupId);
        requireMembership(groupId, email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        return toResponse(groupMessageRepository.save(new GroupMessage(groupId, user, request.message())));
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(Long groupId, String email, Pageable page) {
        requireMembership(groupId, email);
        return groupMessageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, page).map(ChatService::toResponse);
    }

    @Transactional
    public void deliverMessage(ChatMessageResponse message) {
        Long groupId = message.groupId();
        // Use current membership after the message commits, including for sockets that were already open.
        lockGroup(groupId);
        for (var member : groupMemberRepository.findByGroupId(groupId)) {
            messagingTemplate.convertAndSendToUser(member.getUser().getEmail(), "/queue/group/" + groupId, message);
        }
    }

    private void requireMembership(Long groupId, String email) {
        if (!groupMemberRepository.existsByGroupIdAndUserEmail(groupId, email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chat requires group membership");
        }
    }

    private void lockGroup(Long groupId) {
        groupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private static ChatMessageResponse toResponse(GroupMessage message) {
        return new ChatMessageResponse(message.getId(), message.getGroupId(), message.getSender().getId(),
                message.getSender().getName(), message.getMessage(), message.getCreatedAt());
    }
}
