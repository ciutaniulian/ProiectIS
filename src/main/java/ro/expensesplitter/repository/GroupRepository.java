package ro.expensesplitter.repository;


import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.expensesplitter.entity.Group;

@Repository
@Transactional
public interface GroupRepository extends JpaRepository<Group, Long> {
}
