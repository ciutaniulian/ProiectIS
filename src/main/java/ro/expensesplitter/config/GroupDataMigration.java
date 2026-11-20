package ro.expensesplitter.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

@Component
@DependsOnDatabaseInitialization
public class GroupDataMigration implements ApplicationRunner {
    private static final long INITIAL_GROUP_ID = 1000;
    private static final String GROUP_SEQUENCE = "expense_groups";
    private static final String LEGACY_MEMBERS_IMPORTED = "group_roles_members_imported";
    private static final Map<Long, String> LEGACY_NAMES = Map.of(
            1L, "Weekend in Brașov", 2L, "Apartment", 3L, "University friends");

    private final JdbcTemplate jdbc;

    public GroupDataMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void run(ApplicationArguments arguments) {
        var schema = readLegacySchema();
        if (schema.creatorRequired()) {
            jdbc.execute(schema.mysql()
                    ? "alter table expense_groups modify column created_by bigint null"
                    : "alter table expense_groups alter column created_by drop not null");
        }
        lockSequence();
        importRoleBranchMemberships(schema);
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

    private void importRoleBranchMemberships(LegacySchema schema) {
        if (!schema.members() || !jdbc.queryForList(
                "select next_val from expense_group_sequence where sequence_name = ?",
                Long.class, LEGACY_MEMBERS_IMPORTED).isEmpty()) {
            return;
        }
        if (schema.creator()) {
            jdbc.update("update expense_groups set owner_id = created_by where owner_id is null and created_by is not null");
        }
        jdbc.update("""
                insert into group_memberships (group_id, user_id)
                select legacy.group_id, legacy.user_id from group_members legacy
                where not exists (
                    select 1 from group_memberships member
                    where member.group_id = legacy.group_id and member.user_id = legacy.user_id
                )
                """);
        // Record the import in the same transaction so restarts cannot reinstate removed members.
        jdbc.update("insert into expense_group_sequence (sequence_name, next_val) values (?, 0)",
                LEGACY_MEMBERS_IMPORTED);
    }

    private LegacySchema readLegacySchema() {
        return jdbc.execute((ConnectionCallback<LegacySchema>) connection -> {
            var metadata = connection.getMetaData();
            var catalog = connection.getCatalog();
            var schema = connection.getSchema();
            String groups = identifier(metadata, "expense_groups");
            String members = identifier(metadata, "group_members");
            String creator = identifier(metadata, "created_by");
            try (var columns = metadata.getColumns(catalog, schema, groups, creator);
                 var legacy = metadata.getTables(catalog, schema, members, new String[]{"TABLE", "BASE TABLE"})) {
                boolean hasCreator = columns.next();
                return new LegacySchema(
                        metadata.getDatabaseProductName().equals("MySQL"), legacy.next(), hasCreator,
                        hasCreator && columns.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls);
            }
        });
    }

    private static String identifier(DatabaseMetaData metadata, String name) throws SQLException {
        return metadata.storesUpperCaseIdentifiers() ? name.toUpperCase(Locale.ROOT) : name;
    }

    private record LegacySchema(boolean mysql, boolean members, boolean creator, boolean creatorRequired) {}

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
