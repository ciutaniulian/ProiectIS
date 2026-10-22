package ro.expensesplitter.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import ro.expensesplitter.entity.GroupMessage;
import ro.expensesplitter.entity.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@DataJpaTest
public class GroupMessageRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupMessageRepository groupMessageRepository;

    private User user;

    @BeforeEach
    void setup(){
        user = userRepository.save(new User("Ana", "ana@test.com", "password"));
    }

    @Test
    void saveMessageAndSetCreatedAtTest()
    {
        GroupMessage groupMessage = new GroupMessage(1L, user, "test");

        GroupMessage saved = groupMessageRepository.save(groupMessage);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        assertEquals("test",saved.getMessage());
    }
}
