package ro.expensesplitter.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "expense_groups")
public class ExpenseGroup {
    @Id
    @TableGenerator(name = "expense_group_ids", table = "expense_group_sequence",
            pkColumnName = "sequence_name", valueColumnName = "next_val", pkColumnValue = "expense_groups",
            initialValue = 1000, allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "expense_group_ids")
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private GroupStatus status;

    @Column(name = "owner_id")
    private Long ownerId;

    protected ExpenseGroup() {}

    public ExpenseGroup(String name, String description, Long creatorId) {
        this.name = name;
        this.description = description;
        this.status = GroupStatus.ACTIVE;
        this.ownerId = creatorId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public GroupStatus getStatus() {
        return status == null ? GroupStatus.ACTIVE : status;
    }

    public void updateDetails(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void changeStatus(GroupStatus status) {
        this.status = status;
    }
}
