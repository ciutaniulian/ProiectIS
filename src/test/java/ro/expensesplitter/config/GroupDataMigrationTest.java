package ro.expensesplitter.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ro.expensesplitter.dto.GroupResponse;
import ro.expensesplitter.entity.GroupStatus;
import ro.expensesplitter.entity.User;
import ro.expensesplitter.repository.ExpenseGroupRepository;
import ro.expensesplitter.repository.UserRepository;
import ro.expensesplitter.service.GroupService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GroupDataMigrationTest {
    @Autowired private GroupDataMigration migration;
    @Autowired private GroupService service;
    @Autowired private ExpenseGroupRepository groups;
    @Autowired private UserRepository users;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void preservesLegacyIdsAndAccessAndCanRunRepeatedly() {
        long legacyGroup = 9001;
        long existingGroup = 9002;
        long messageOnlyGroup = 9003;
        var member = users.save(new User("Legacy member", UUID.randomUUID() + "@example.com", "unused"));
        jdbc.update("insert into expense_groups (id, name, owner_id) values (?, ?, ?)", existingGroup,
                "Existing group", member.getId());
        jdbc.update("insert into group_memberships (group_id, user_id) values (?, ?)", legacyGroup, member.getId());
        jdbc.update("insert into group_memberships (group_id, user_id) values (?, ?)", existingGroup, member.getId());
        for (long groupId : new long[]{legacyGroup, messageOnlyGroup}) {
            jdbc.update("""
                    insert into group_messages (group_id, sender_id, message, created_at)
                    values (?, ?, ?, current_timestamp)
                    """, groupId, member.getId(), "Legacy message");
        }

        migration.run(new DefaultApplicationArguments());
        migration.run(new DefaultApplicationArguments());

        var visible = service.list(member.getEmail());
        assertThat(visible).extracting(GroupResponse::id).containsExactly(existingGroup, legacyGroup);
        assertThat(visible).extracting(GroupResponse::status).containsOnly(GroupStatus.ACTIVE);
        assertThat(service.details(legacyGroup, member.getEmail()).group().name()).isEqualTo("Group 9001");
        assertThat(service.details(existingGroup, member.getEmail()).group().name()).isEqualTo("Existing group");
        assertThat(groups.existsById(messageOnlyGroup)).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from group_messages where group_id in (?, ?)", Long.class,
                legacyGroup, messageOnlyGroup)).isEqualTo(2);

        assertThat(service.create("After migration", null, member.getEmail()).id()).isGreaterThan(messageOnlyGroup);
    }
}
