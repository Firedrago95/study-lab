package concurrency.practice.service;

import static concurrency.practice.exception.PracticeErrorCode.INVENTORY_NOT_FOUND;
import static concurrency.practice.exception.PracticeErrorCode.OPTIMISTIC_LOCK_CONFLICT;
import static concurrency.practice.exception.PracticeErrorCode.ORDER_NOT_FOUND;

import concurrency.practice.domain.Inventory;
import concurrency.practice.domain.ProductOrder;
import concurrency.practice.exception.PracticeException;
import concurrency.practice.repository.InventoryRepository;
import concurrency.practice.repository.ProductOrderRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class OrderService {

    private final InventoryRepository inventoryRepository;
    private final ProductOrderRepository productOrderRepository;

    /**
     * 비관적 락 예제: 주문 생성 시 재고 동시성 문제 해결
     */
    @Transactional
    public Long createOrder(Long inventoryId, int quantity) {
        // PESSIMISTIC_WRITE 잠금을 통해 다른 트랜잭션의 동시 접근을 막는다.
        Inventory inventory = inventoryRepository.findByIdWithPessimisticLock(inventoryId)
                .orElseThrow(() -> new PracticeException(INVENTORY_NOT_FOUND));

        // 재고 감소
        inventory.decrease(quantity);

        // 주문 생성
        ProductOrder order = new ProductOrder(inventoryId, quantity);
        ProductOrder savedOrder = productOrderRepository.save(order);

        return savedOrder.getId();
    }

    /**
     * 낙관적 락 예제: 주문 수정 시 동시성 문제 해결
     */
    @Transactional
    public void updateOrderQuantity(Long orderId, int newQuantity) {
        try {
            ProductOrder order = productOrderRepository.findById(orderId)
                    .orElseThrow(() -> new PracticeException(ORDER_NOT_FOUND));

            order.updateQuantity(newQuantity);

            // saveAndFlush()를 호출하여 명시적으로 DB에 UPDATE 쿼리를 보내고,
            // 버전 충돌 시 OptimisticLockException을 즉시 발생시킨다.
            // 트랜잭션 커밋 시점에 확인해도 되지만, 빠른 예외 발생을 위해 명시적으로 호출.
            productOrderRepository.saveAndFlush(order);

        } catch (OptimisticLockException e) {
            // 버전 충돌 발생 시, 사용자 정의 예외로 변환하여 던진다.
            throw new PracticeException(OPTIMISTIC_LOCK_CONFLICT);
        }
    }
}
