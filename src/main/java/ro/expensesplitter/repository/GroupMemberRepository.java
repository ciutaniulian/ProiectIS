package ro.expensesplitter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.expensesplitter.entity.Group;
import ro.expensesplitter.entity.GroupMember;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    @Query("select gm.group from GroupMember gm where gm.user.id = :userId")
    List<Group> findGroupsByUserId(@Param("userId") Long userId);

    @Query("select gm from GroupMember gm join fetch gm.user where gm.group.id = :groupId")
    List<GroupMember> findByGroupId(@Param("groupId") Long groupId);
}
