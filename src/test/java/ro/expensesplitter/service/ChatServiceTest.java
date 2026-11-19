package ro.expensesplitter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.dto.SendMessageRequest;
import ro.expensesplitter.entity.Group;
import ro.expensesplitter.entity.GroupMember;
import ro.expensesplitter.entity.GroupMessage;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.GroupMemberRepository;
import ro.expensesplitter.repository.GroupMessageRepository;
import ro.expensesplitter.repository.GroupRepository;
import ro.expensesplitter.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    private static final long GROUP_ID = 1L;
    private final GroupMessageRepository messages = mock(GroupMessageRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final GroupMemberRepository members = mock(GroupMemberRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final ChatService service = new ChatService(messages, users, members, groups, messaging);
    private final User ana = new User("Ana", "ana@example.com", "password");

    @BeforeEach
    void groupExists() {
        when(groups.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(Group.builder().id(GROUP_ID).build()));
    }

    @Test
    void memberCanSendAndReadMessages() {
        var request = new SendMessageRequest("Hello");
        var page = PageRequest.of(0, 20);
        when(users.findByEmail(ana.getEmail())).thenReturn(Optional.of(ana));
        when(members.existsByGroupIdAndUserEmail(GROUP_ID, ana.getEmail())).thenReturn(true);
        when(messages.save(any(GroupMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messages.findByGroupIdOrderByCreatedAtDesc(GROUP_ID, page))
                .thenReturn(new PageImpl<>(List.of(new GroupMessage(GROUP_ID, ana, "Hello"))));

        assertEquals("Hello", service.sendMessage(GROUP_ID, ana.getEmail(), request).message());
        assertEquals("Ana", service.getMessages(GROUP_ID, ana.getEmail(), page).getContent().get(0).senderName());
    }

    @Test
    void formerMemberCannotSendOrReadMessages() {
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.sendMessage(GROUP_ID, ana.getEmail(), new SendMessageRequest("Hello"))).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.getMessages(GROUP_ID, ana.getEmail(), PageRequest.of(0, 20))).getStatusCode().value());
        verifyNoInteractions(messages);
    }

    @Test
    void deliveryUsesOnlyCurrentMembers() {
        var message = new ChatMessageResponse(1L, GROUP_ID, 3L, "Ana", "Hello", LocalDateTime.now());
        when(members.findByGroupId(GROUP_ID)).thenReturn(List.of(GroupMember.builder().user(ana).build()));

        service.deliverMessage(message);

        verify(messaging).convertAndSendToUser(ana.getEmail(), "/queue/group/1", message);
        org.mockito.Mockito.verifyNoMoreInteractions(messaging);
    }
}
