package ro.expensesplitter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.expensesplitter.entity.GroupMember;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
}
