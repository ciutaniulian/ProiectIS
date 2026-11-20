package ro.expensesplitter.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ro.expensesplitter.dto.GroupResponse;
import ro.expensesplitter.entity.ExpenseGroup;

import java.util.List;
import java.util.Optional;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {
    @Query("""
            select new ro.expensesplitter.dto.GroupResponse(g.id, g.name, g.description, count(member),
                coalesce(g.status, ro.expensesplitter.entity.GroupStatus.ACTIVE))
            from ExpenseGroup g join GroupMembership member on member.groupId = g.id
            join GroupMembership mine on mine.groupId = g.id
            where mine.user.email = :email
            and (g.status is null or g.status <> ro.expensesplitter.entity.GroupStatus.CLOSED)
            group by g.id, g.name, g.description, g.status
            order by g.id desc
            """)
    List<GroupResponse> findForMember(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from ExpenseGroup g where g.id = :id")
    Optional<ExpenseGroup> findForUpdate(Long id);
}
