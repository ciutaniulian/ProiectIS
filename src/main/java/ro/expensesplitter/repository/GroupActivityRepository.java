package ro.expensesplitter.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.expensesplitter.entity.GroupActivity;

public interface GroupActivityRepository extends JpaRepository<GroupActivity, Long> {
    Slice<GroupActivity> findByGroupIdOrderByIdDesc(Long groupId, Pageable page);
}
