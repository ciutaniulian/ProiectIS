package ro.expensesplitter.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import ro.expensesplitter.service.GroupChatAccess;

import java.util.regex.Pattern;

@Component
public class ChatInboundAuthorization implements ChannelInterceptor {
    static final Pattern GROUP_TOPIC = Pattern.compile("/topic/group/([1-9][0-9]{0,18})");
    static final String REPLY_QUEUE = "/queue/chat/replies";
    private static final String USER_REPLY_QUEUE = "/user" + REPLY_QUEUE;
    private static final Pattern CHAT_SEND = Pattern.compile("/app/chat/([1-9][0-9]{0,18})(?:/ready)?");

    private final GroupChatAccess access;
    private final ChatOutboundAuthorization outbound;

    public ChatInboundAuthorization(GroupChatAccess access, ChatOutboundAuthorization outbound) {
        this.access = access;
        this.outbound = outbound;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null || headers.getCommand() == null) {
            return message;
        }
        StompCommand command = headers.getCommand();
        if (command == StompCommand.SEND || command == StompCommand.SUBSCRIBE) {
            Pattern allowedDestination = command == StompCommand.SEND ? CHAT_SEND : GROUP_TOPIC;
            String destination = headers.getDestination();
            if (destination == null) {
                throw new AccessDeniedException("Invalid chat destination");
            }
            if (command == StompCommand.SUBSCRIBE && USER_REPLY_QUEUE.equals(destination) && headers.getUser() != null) {
                return message;
            }
            var match = allowedDestination.matcher(destination);
            if (!match.matches()) {
                throw new AccessDeniedException("Invalid chat destination");
            }
            long groupId;
            try {
                groupId = Long.parseLong(match.group(1));
            } catch (NumberFormatException ex) {
                throw new AccessDeniedException("Invalid group ID", ex);
            }
            if (command == StompCommand.SUBSCRIBE) {
                long membershipId = access.membershipId(groupId, headers.getUser())
                        .orElseThrow(() -> new AccessDeniedException("Group membership required"));
                outbound.rememberSubscription(headers.getSessionId(), groupId, membershipId);
            } else {
                access.requireMember(groupId, headers.getUser());
            }
        }
        return message;
    }
}
