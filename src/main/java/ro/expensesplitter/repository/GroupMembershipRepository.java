package ro.expensesplitter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ro.expensesplitter.entity.GroupMembership;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    boolean existsByGroupIdAndUserEmail(Long groupId, String email);

    boolean existsByGroupId(Long groupId);

    long countByGroupId(Long groupId);

    @Query("select m.id from GroupMembership m where m.groupId = :groupId and m.user.email = :email")
    Optional<Long> findMembershipId(Long groupId, String email);

    @Query("select m from GroupMembership m join fetch m.user where m.groupId = :groupId order by m.id")
    List<GroupMembership> findMembers(Long groupId);

    @Query("select membership.groupId from GroupMembership membership where membership.user.email = :email")
    List<Long> findGroupIdsByEmail(String email);
}
