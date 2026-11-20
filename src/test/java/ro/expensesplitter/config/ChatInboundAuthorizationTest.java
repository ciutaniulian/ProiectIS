package ro.expensesplitter.config;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import ro.expensesplitter.repository.GroupMembershipRepository;
import ro.expensesplitter.service.GroupChatAccess;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatInboundAuthorizationTest {
    private final GroupMembershipRepository memberships = mock(GroupMembershipRepository.class);
    private final ChatInboundAuthorization authorization = new ChatInboundAuthorization(
            new GroupChatAccess(memberships), mock(ChatOutboundAuthorization.class));
    private final Principal user = () -> "member@example.com";

    @Test
    void allowsMembersToSubscribeAndSendOnlyInTheirGroup() {
        when(memberships.existsByGroupIdAndUserEmail(1L, user.getName())).thenReturn(true);
        when(memberships.findMembershipId(1L, user.getName())).thenReturn(Optional.of(10L));

        assertAllowed(StompCommand.SUBSCRIBE, "/topic/group/1");
        assertAllowed(StompCommand.SUBSCRIBE, "/user/queue/chat/replies");
        assertAllowed(StompCommand.SEND, "/app/chat/1");
        assertAllowed(StompCommand.SEND, "/app/chat/1/ready");
        assertDenied(StompCommand.SUBSCRIBE, "/topic/group/2");
        assertDenied(StompCommand.SEND, "/app/chat/2");
    }

    @Test
    void deniesWildcardSubscriptionsAndClientPublishingToBrokerTopics() {
        when(memberships.existsByGroupIdAndUserEmail(1L, user.getName())).thenReturn(true);

        assertDenied(StompCommand.SUBSCRIBE, "/topic/group/*");
        assertDenied(StompCommand.SUBSCRIBE, "/queue/chat/replies-user-other");
        assertDenied(StompCommand.SEND, "/topic/group/1");
    }

    private void assertAllowed(StompCommand command, String destination) {
        Message<byte[]> message = message(command, destination);
        assertSame(message, authorization.preSend(message, null));
    }

    private void assertDenied(StompCommand command, String destination) {
        Message<byte[]> message = message(command, destination);
        assertThrows(AccessDeniedException.class, () -> authorization.preSend(message, null));
    }

    private Message<byte[]> message(StompCommand command, String destination) {
        var headers = StompHeaderAccessor.create(command);
        headers.setDestination(destination);
        headers.setUser(user);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }
}
