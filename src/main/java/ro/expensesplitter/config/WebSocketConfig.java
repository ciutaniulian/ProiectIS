package ro.expensesplitter.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import ro.expensesplitter.repository.GroupMemberRepository;

import java.util.regex.Pattern;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Pattern CHAT_SEND = Pattern.compile("/app/chat/([0-9]{1,18})");
    private static final Pattern CHAT_SUBSCRIPTION = Pattern.compile("/user/queue/group/([0-9]{1,18})");

    private final GroupMemberRepository groupMemberRepository;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws");
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (headers == null) return message;

                if (headers.getCommand() == StompCommand.SEND) {
                    requireChatAccess(headers, CHAT_SEND);
                } else if (headers.getCommand() == StompCommand.SUBSCRIBE) {
                    requireChatAccess(headers, CHAT_SUBSCRIPTION);
                }
                return message;
            }
        });
    }

    private void requireChatAccess(StompHeaderAccessor headers, Pattern path) {
        String destination = headers.getDestination();
        if (headers.getUser() == null || destination == null) {
            throw new AccessDeniedException("Chat requires group membership");
        }

        var match = path.matcher(destination);
        if (!match.matches() ||
                !groupMemberRepository.existsByGroupIdAndUserEmail(Long.valueOf(match.group(1)), headers.getUser().getName())) {
            throw new AccessDeniedException("Chat requires group membership");
        }
    }
}
