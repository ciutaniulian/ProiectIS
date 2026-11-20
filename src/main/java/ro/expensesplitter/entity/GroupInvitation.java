package ro.expensesplitter.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "group_invitations", indexes = {
        @Index(name = "idx_invite_group_status", columnList = "group_id, status, expires_at"),
        @Index(name = "idx_invite_email_status", columnList = "email, status, expires_at")
})
public class GroupInvitation {
    public enum Status { PENDING, ACCEPTED, DECLINED, REVOKED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected GroupInvitation() {}

    public GroupInvitation(ExpenseGroup group, String email, String tokenHash, Instant expiresAt) {
        this.group = group;
        this.email = email;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.status = Status.PENDING;
    }

    public Long getId() { return id; }
    public ExpenseGroup getGroup() { return group; }
    public String getEmail() { return email; }
    public Instant getExpiresAt() { return expiresAt; }

    public Status statusAt(Instant now) {
        if (status == Status.PENDING && !expiresAt.isAfter(now)) {
            return Status.EXPIRED;
        }
        return status;
    }

    public void changeStatus(Status status) {
        this.status = status;
    }
}
