package ro.expensesplitter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.expensesplitter.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
