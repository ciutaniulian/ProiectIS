package ro.expensesplitter.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "group_messages", indexes = {@Index(name = "idx_group_created", columnList = "group_id, created_at")})
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id",nullable = false)
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "message",nullable = false)
    private String message;

    @Column(name = "created_at",nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public GroupMessage(){}

    public GroupMessage(Long groupId, User sender, String message) {
        this.groupId = groupId;
        this.sender = sender;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public User getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
