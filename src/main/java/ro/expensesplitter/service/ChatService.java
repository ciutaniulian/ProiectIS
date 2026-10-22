package ro.expensesplitter.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.dto.SendMessageRequest;
import ro.expensesplitter.entity.GroupMessage;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.GroupMessageRepository;
import ro.expensesplitter.repository.UserRepository;

import java.util.stream.Collectors;


@Service
public class ChatService {

        private final GroupMessageRepository groupMessageRepository;

        private final UserRepository userRepository;

       public ChatService(GroupMessageRepository groupMessageRepository, UserRepository userRepository) {
           this.groupMessageRepository = groupMessageRepository;
           this.userRepository = userRepository;
       }

       public ChatMessageResponse sendMessage(Long groupId, Long userId, SendMessageRequest request) {
           User user = userRepository.findById(userId).orElseThrow();

           GroupMessage message = new GroupMessage(groupId, user, request.message());

           groupMessageRepository.save(message);

           return new ChatMessageResponse(message.getId(), groupId, user.getId(), user.getName(), message.getMessage(), message.getCreatedAt());
       }

    public ChatMessageResponse sendMessage(Long groupId, String email, SendMessageRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        GroupMessage message = new GroupMessage(groupId, user, request.message());
        groupMessageRepository.save(message);

        return new ChatMessageResponse(
                message.getId(),
                groupId,
                user.getId(),
                user.getName(),
                message.getMessage(),
                message.getCreatedAt()
        );
    }

       public Page<ChatMessageResponse> getMessages(Long groupId, Pageable page){

           Page<GroupMessage> messages = groupMessageRepository.findByGroupIdOrderByCreatedAtDesc(groupId, page);
           return messages.map(message -> new ChatMessageResponse(message.getId(), groupId, message.getSender().getId(), message.getSender().getName(), message.getMessage(), message.getCreatedAt()));
       }


}
