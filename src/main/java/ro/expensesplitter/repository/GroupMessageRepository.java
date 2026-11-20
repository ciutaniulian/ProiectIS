package ro.expensesplitter.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.expensesplitter.entity.GroupMessage;

import java.util.List;
import java.util.Optional;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    Page<GroupMessage> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);

    List<GroupMessage> findByGroupIdAndIdLessThanOrderByIdDesc(Long groupId, Long before, Pageable pageable);

    List<GroupMessage> findByGroupIdAndIdGreaterThanOrderByIdAsc(Long groupId, Long after, Pageable pageable);

    List<GroupMessage> findByGroupIdOrderByIdDesc(Long groupId, Pageable pageable);

    Optional<GroupMessage> findByGroupIdAndSenderIdAndClientMessageId(Long groupId, Long senderId, String clientMessageId);

    boolean existsByGroupId(Long groupId);
}
