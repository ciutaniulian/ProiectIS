package ro.expensesplitter.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "group_activity", indexes = @Index(name = "idx_activity_group_id", columnList = "group_id, id"))
public class GroupActivity {
    public enum Type { CREATED, UPDATED, JOINED, LEFT, REMOVED, ARCHIVED, RESTORED, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Type type;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "actor_name", nullable = false, length = 100)
    private String actorName;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "subject_name", length = 100)
    private String subjectName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GroupActivity() {}

    public GroupActivity(Long groupId, Type type, User actor, User subject, Instant createdAt) {
        this.groupId = groupId;
        this.type = type;
        this.actorId = actor.getId();
        this.actorName = actor.getName();
        if (subject != null) {
            this.subjectId = subject.getId();
            this.subjectName = subject.getName();
        }
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Type getType() { return type; }
    public String getActorName() { return actorName; }
    public String getSubjectName() { return subjectName; }
    public Instant getCreatedAt() { return createdAt; }
}
