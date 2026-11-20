package ro.expensesplitter.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import ro.expensesplitter.repository.GroupMembershipRepository;
import ro.expensesplitter.service.GroupAccessChanged;
import ro.expensesplitter.service.GroupChatAccess;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatOutboundAuthorizationTest {
    @Test
    void dropsMessagesAfterMembershipIsRevokedWithoutClosingTheSession() throws Exception {
        var memberships = mock(GroupMembershipRepository.class);
        var authorization = new ChatOutboundAuthorization(new GroupChatAccess(memberships));
        var handler = authorization.trackRecipients(mock(WebSocketHandler.class));
        Principal recipient = () -> "recipient@example.com";
        Principal sender = () -> "sender@example.com";
        var session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("recipient-session");
        when(session.getPrincipal()).thenReturn(recipient);
        when(memberships.findMembershipId(1L, recipient.getName())).thenReturn(Optional.of(10L));
        when(memberships.findMembershipId(1L, sender.getName())).thenReturn(Optional.of(11L));
        handler.afterConnectionEstablished(session);
        authorization.rememberSubscription(session.getId(), 1L, 10L);

        var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(session.getId());
        headers.setDestination("/topic/group/1");
        headers.setUser(sender);
        Message<String> message = MessageBuilder.createMessage("hello", headers.getMessageHeaders());
        assertSame(message, authorization.beforeHandle(message, null, null));

        when(memberships.findMembershipId(1L, recipient.getName())).thenReturn(Optional.empty());
        assertNull(authorization.beforeHandle(message, null, null));

        when(memberships.findMembershipId(1L, recipient.getName())).thenReturn(Optional.of(12L));
        assertNull(authorization.beforeHandle(message, null, null));
        authorization.onGroupAccessChanged(new GroupAccessChanged(1L, recipient.getName()));
        verify(session).close(argThat(status -> status.getCode() == 4003));
        assertNull(authorization.beforeHandle(message, null, null));
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
    }
}
