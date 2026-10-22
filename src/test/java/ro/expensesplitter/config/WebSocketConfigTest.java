package ro.expensesplitter.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WebSocketConfigTest {

    private MessageBrokerRegistry registry;

    @BeforeEach
    void setup(){
        registry = mock(MessageBrokerRegistry.class);
    }

    @Test
    void messageBrokerRegistryTest(){

        WebSocketConfig config = new WebSocketConfig();
        config.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic");
        verify(registry).setApplicationDestinationPrefixes("/app");
    }

}
