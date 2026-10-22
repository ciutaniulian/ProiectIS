package ro.expensesplitter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.dto.SendMessageRequest;
import ro.expensesplitter.entity.GroupMessage;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.GroupMessageRepository;
import ro.expensesplitter.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ChatServiceTest {

    private GroupMessageRepository groupMessageRepository;
    private UserRepository userRepository;
    private ChatService chatService;

    @BeforeEach
    void setup(){
        groupMessageRepository=mock(GroupMessageRepository.class);
        userRepository=mock(UserRepository.class);

        chatService = new ChatService(groupMessageRepository,userRepository);
    }

    @Test
    void sendMessageSuccsessfullyTest()
    {
        Long groupId = 1L;
        Long userId = 10L;

        User user = new User("Ana", "ana@gmail.com", "password");

        SendMessageRequest request = new SendMessageRequest("test");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(groupMessageRepository.save(any(GroupMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessageResponse response =  chatService.sendMessage(groupId, userId, request);

        assertNotNull(response);
        assertEquals("test",response.message());
        assertEquals("Ana", response.senderName());
        assertEquals(groupId, response.groupId());

        verify(groupMessageRepository,times(1)).save(any(GroupMessage.class));
    }

    @Test
    void getPastMessagesTest()
    {
        Long groupId = 1L;
        Pageable page = PageRequest.of(0,20);
        User user = new User("Ana","ana@test.com","password");
        GroupMessage message = new GroupMessage(1L,user,"test");

        Page<GroupMessage> messagePage = new PageImpl<>(List.of(message));

        when(groupMessageRepository.findByGroupIdOrderByCreatedAtDesc(groupId,page)).thenReturn(messagePage);

        Page<ChatMessageResponse> responses=chatService.getMessages(groupId,page);

        assertNotNull(responses);
        assertEquals(1,responses.getTotalElements());
        assertEquals("test",responses.getContent().get(0).message());
        assertEquals("Ana",responses.getContent().get(0).senderName());
    }
}
