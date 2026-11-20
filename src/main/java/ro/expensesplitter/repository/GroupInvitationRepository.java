package ro.expensesplitter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ro.expensesplitter.entity.GroupInvitation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {
    boolean existsByGroupIdAndEmailAndStatusAndExpiresAtAfter(
            Long groupId, String email, GroupInvitation.Status status, Instant now);

    List<GroupInvitation> findByGroupIdAndStatusAndExpiresAtAfterOrderByIdDesc(
            Long groupId, GroupInvitation.Status status, Instant now);

    @Query("""
            select i from GroupInvitation i join fetch i.group g
            where i.email = :email and i.status = :status
                and i.expiresAt > :now and g.status = ro.expensesplitter.entity.GroupStatus.ACTIVE
            order by i.id desc
            """)
    List<GroupInvitation> findInbox(String email, GroupInvitation.Status status, Instant now);

    @Query("select i from GroupInvitation i join fetch i.group where i.tokenHash = :tokenHash and i.email = :email")
    Optional<GroupInvitation> findForRecipientByToken(String tokenHash, String email);

    @Query("select i.group.id from GroupInvitation i where i.id = :id and i.email = :email")
    Optional<Long> findGroupIdForRecipient(Long id, String email);

    Optional<GroupInvitation> findByIdAndGroupId(Long id, Long groupId);
}
