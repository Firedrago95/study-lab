package concurrency.practice.repository;

import concurrency.practice.domain.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * 비관적 쓰기 잠금을 사용하여 재고 정보를 조회합니다.
     * 다른 트랜잭션은 이 레코드에 대한 잠금이 해제될 때까지 읽기는 가능하지만, 수정/삭제는 대기해야 합니다.
     * 이를 통해 동시 재고 감소 요청 시 데이터 정합성을 보장할 수 있습니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.id = :id")
    Optional<Inventory> findByIdWithPessimisticLock(Long id);
}
