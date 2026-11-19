package ro.expensesplitter.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import ro.expensesplitter.repository.GroupMemberRepository;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    private final GroupMemberRepository members = mock(GroupMemberRepository.class);
    private final WebSocketConfig config = new WebSocketConfig(members);

    @Test
    void configuresPrivateQueues() {
        var registry = mock(MessageBrokerRegistry.class);
        config.configureMessageBroker(registry);
        verify(registry).enableSimpleBroker("/queue");
        verify(registry).setApplicationDestinationPrefixes("/app");
    }

    @Test
    void allowsSubscriptionsOnlyForMembersOnTheirUserDestination() {
        var registration = mock(ChannelRegistration.class);
        config.configureClientInboundChannel(registration);
        var captor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        verify(registration).interceptors(captor.capture());
        var interceptor = captor.getValue();
        var channel = mock(MessageChannel.class);

        when(members.existsByGroupIdAndUserEmail(2L, "ana@example.com")).thenReturn(true);
        var ownQueue = frame(StompCommand.SUBSCRIBE, "/user/queue/group/2");
        assertSame(ownQueue, interceptor.preSend(ownQueue, channel));

        var otherGroup = frame(StompCommand.SUBSCRIBE, "/user/queue/group/3");
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(otherGroup, channel));

        var directSend = frame(StompCommand.SEND, "/queue/group/2");
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(directSend, channel));
    }

    private static Message<byte[]> frame(StompCommand command, String destination) {
        var headers = StompHeaderAccessor.create(command);
        headers.setDestination(destination);
        headers.setUser(() -> "ana@example.com");
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }
}
