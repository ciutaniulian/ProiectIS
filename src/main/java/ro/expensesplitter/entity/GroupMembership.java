package ro.expensesplitter.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "group_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
public class GroupMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected GroupMembership() {}

    public GroupMembership(Long groupId, User user) {
        this.groupId = groupId;
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
