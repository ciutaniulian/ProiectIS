package ro.expensesplitter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import ro.expensesplitter.dto.ChatMessageResponse;
import ro.expensesplitter.dto.SendMessageRequest;
import ro.expensesplitter.entity.ExpenseGroup;
import ro.expensesplitter.entity.GroupMessage;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.entity.GroupStatus;
import ro.expensesplitter.repository.ExpenseGroupRepository;
import ro.expensesplitter.repository.GroupMessageRepository;
import ro.expensesplitter.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ChatServiceTest {

    private GroupMessageRepository groupMessageRepository;
    private UserRepository userRepository;
    private ChatService chatService;
    private GroupChatAccess access;
    private ExpenseGroupRepository groups;
    private ExpenseGroup group;
    private final Principal principal = () -> "ana@gmail.com";

    @BeforeEach
    void setup(){
        groupMessageRepository=mock(GroupMessageRepository.class);
        userRepository=mock(UserRepository.class);

        access = mock(GroupChatAccess.class);
        groups = mock(ExpenseGroupRepository.class);
        group = mock(ExpenseGroup.class);
        when(groups.findForUpdate(1L)).thenReturn(Optional.of(group));
        when(group.getStatus()).thenReturn(GroupStatus.ACTIVE);
        chatService = new ChatService(groupMessageRepository,userRepository,access,groups);
    }

    @Test
    void sendMessageSuccsessfullyTest()
    {
        Long groupId = 1L;

        User user = new User("Ana", "ana@gmail.com", "password");

        SendMessageRequest request = new SendMessageRequest("test");

        when(userRepository.findByEmail(principal.getName())).thenReturn(Optional.of(user));

        when(groupMessageRepository.save(any(GroupMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatMessageResponse response = chatService.sendMessage(groupId, principal, request);

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

        Page<ChatMessageResponse> responses=chatService.getMessages(groupId,principal,page);

        assertNotNull(responses);
        assertEquals(1,responses.getTotalElements());
        assertEquals("test",responses.getContent().get(0).message());
        assertEquals("Ana",responses.getContent().get(0).senderName());
    }

    @Test
    void rejectsNonMembersBeforeReadingOrWritingMessages() {
        doThrow(new AccessDeniedException("Group membership required")).when(access).requireMember(1L, principal);

        assertThrows(AccessDeniedException.class, () -> chatService.getMessages(1L, principal, PageRequest.of(0, 20)));
        assertThrows(AccessDeniedException.class, () -> chatService.sendMessage(1L, principal, new SendMessageRequest("hello")));
        verifyNoInteractions(groupMessageRepository, userRepository);
    }

    @Test
    void archivedMembersCanReadHistoryButCannotSend() {
        when(group.getStatus()).thenReturn(GroupStatus.ARCHIVED);
        var page = PageRequest.of(0, 2);
        when(groupMessageRepository.findByGroupIdOrderByIdDesc(1L, page)).thenReturn(List.of());

        assertEquals(List.of(), chatService.getHistory(1L, principal, null, null, 1).messages());
        var failure = assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessage(1L, principal, new SendMessageRequest("hello")));
        assertEquals(409, failure.getStatusCode().value());
        verifyNoInteractions(userRepository);
        verify(groupMessageRepository, never()).save(any());
    }

    @Test
    void retryWithSameRequestReturnsTheMessageAlreadyStored() {
        var sender = mock(User.class);
        when(sender.getId()).thenReturn(12L);
        when(userRepository.findByEmail(principal.getName())).thenReturn(Optional.of(sender));
        var requestId = UUID.randomUUID();
        var existing = new GroupMessage(1L, sender, "original", requestId.toString());
        when(groupMessageRepository.findByGroupIdAndSenderIdAndClientMessageId(1L, 12L, requestId.toString()))
                .thenReturn(Optional.of(existing));

        var response = chatService.sendMessage(1L, principal, new SendMessageRequest("original", requestId));
        assertEquals("original", response.message());
        verify(groupMessageRepository, never()).save(any());
    }
}
