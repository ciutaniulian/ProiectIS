package ro.expensesplitter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import ro.expensesplitter.service.GroupAccessChanged;
import ro.expensesplitter.service.GroupChatAccess;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ChatOutboundAuthorization implements ExecutorChannelInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ChatOutboundAuthorization.class);
    private static final CloseStatus ACCESS_REVOKED = new CloseStatus(4003, "Group membership ended");
    private final GroupChatAccess access;
    private final ConcurrentMap<String, Recipient> recipients = new ConcurrentHashMap<>();

    public ChatOutboundAuthorization(GroupChatAccess access) {
        this.access = access;
    }

    WebSocketHandler trackRecipients(WebSocketHandler delegate) {
        return new WebSocketHandlerDecorator(delegate) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                Principal principal = session.getPrincipal();
                if (principal != null) {
                    recipients.put(session.getId(), new Recipient(session, principal, new ConcurrentHashMap<>()));
                }
                try {
                    super.afterConnectionEstablished(session);
                } catch (Exception ex) {
                    recipients.remove(session.getId());
                    throw ex;
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                recipients.remove(session.getId());
                super.afterConnectionClosed(session, status);
            }
        };
    }

    void rememberSubscription(String sessionId, long groupId, long membershipId) {
        Recipient recipient = sessionId == null ? null : recipients.get(sessionId);
        if (recipient != null) {
            recipient.memberships().put(groupId, membershipId);
            if (!holdsOriginalMembership(recipient, groupId)) {
                closeRevoked(sessionId, recipient);
            }
        }
    }

    @TransactionalEventListener
    public void onGroupAccessChanged(GroupAccessChanged event) {
        recipients.forEach((sessionId, recipient) -> {
            if (recipient.principal().getName().equals(event.email())
                    && recipient.memberships().containsKey(event.groupId())
                    && !holdsOriginalMembership(recipient, event.groupId())) {
                closeRevoked(sessionId, recipient);
            }
        });
    }

    private void closeRevoked(String sessionId, Recipient recipient) {
        recipients.remove(sessionId, recipient);
        try {
            recipient.session().close(ACCESS_REVOKED);
        } catch (IOException failure) {
            log.debug("Could not close chat session after group access was revoked", failure);
        }
    }

    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler) {
        var headers = message.getHeaders();
        if (SimpMessageHeaderAccessor.getMessageType(headers) != SimpMessageType.MESSAGE) {
            return message;
        }
        String destination = SimpMessageHeaderAccessor.getDestination(headers);
        if (destination == null) {
            return null;
        }
        String sessionId = SimpMessageHeaderAccessor.getSessionId(headers);
        Recipient recipient = sessionId == null ? null : recipients.get(sessionId);
        if (recipient == null) {
            return null;
        }
        if ((ChatInboundAuthorization.REPLY_QUEUE + "-user" + sessionId).equals(destination)) {
            return message;
        }
        var topic = ChatInboundAuthorization.GROUP_TOPIC.matcher(destination);
        if (!topic.matches()) {
            return null;
        }
        // Broker messages identify the recipient by session ID. Their user header
        // can come from the original message and must not authorize the recipient.
        return holdsOriginalMembership(recipient, Long.valueOf(topic.group(1))) ? message : null;
    }

    private boolean holdsOriginalMembership(Recipient recipient, long groupId) {
        Long expectedMembership = recipient.memberships().get(groupId);
        return expectedMembership != null && access.membershipId(groupId, recipient.principal())
                .filter(expectedMembership::equals).isPresent();
    }

    private record Recipient(WebSocketSession session, Principal principal, Map<Long, Long> memberships) {}
}
