package ro.expensesplitter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.expensesplitter.entity.GroupMember;
import ro.expensesplitter.entity.GroupRole;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    long countByGroupId(Long groupId);
    long countByGroupIdAndRole(Long groupId, GroupRole role);

    @Query("select gm from GroupMember gm join fetch gm.group where gm.user.id = :userId order by gm.group.id")
    List<GroupMember> findMembershipsByUserId(@Param("userId") Long userId);

    @Query("select gm from GroupMember gm join fetch gm.user where gm.group.id = :groupId")
    List<GroupMember> findByGroupId(@Param("groupId") Long groupId);
}
