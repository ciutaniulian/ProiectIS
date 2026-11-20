package ro.expensesplitter.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@DependsOnDatabaseInitialization
public class GroupDataMigration implements ApplicationRunner {
    private static final long INITIAL_GROUP_ID = 1000;
    private static final String GROUP_SEQUENCE = "expense_groups";
    private static final Map<Long, String> LEGACY_NAMES = Map.of(
            1L, "Weekend in Brașov", 2L, "Apartment", 3L, "University friends");

    private final JdbcTemplate jdbc;

    public GroupDataMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void run(ApplicationArguments arguments) {
        lockSequence();
        jdbc.update("update expense_groups set status = 'ACTIVE' where status is null");

        var missingGroups = jdbc.queryForList("""
                select legacy.group_id
                from (select group_id from group_memberships union select group_id from group_messages) legacy
                left join expense_groups existing on existing.id = legacy.group_id
                where existing.id is null
                """, Long.class);
        for (Long id : missingGroups) {
            jdbc.update("insert into expense_groups (id, name, status) values (?, ?, 'ACTIVE')",
                    id, LEGACY_NAMES.getOrDefault(id, "Group " + id));
        }

        Long floor = jdbc.queryForObject("select max(id) from expense_groups", Long.class);
        Long previousAllocation = jdbc.queryForObject("select max(next_val) from expense_group_sequence", Long.class);
        long safeValue = Math.max(previousAllocation, floor == null ? INITIAL_GROUP_ID : floor);
        jdbc.update("update expense_group_sequence set next_val = ? where sequence_name = ? and next_val < ?",
                safeValue, GROUP_SEQUENCE, safeValue);
    }

    private void lockSequence() {
        String lock = "select next_val from expense_group_sequence where sequence_name = ? for update";
        if (jdbc.queryForList(lock, Long.class, GROUP_SEQUENCE).isEmpty()) {
            try {
                jdbc.update("insert into expense_group_sequence (sequence_name, next_val) values (?, ?)",
                        GROUP_SEQUENCE, INITIAL_GROUP_ID);
            } catch (DuplicateKeyException concurrentStartup) {
                jdbc.queryForList(lock, Long.class, GROUP_SEQUENCE);
            }
        }
    }
}
