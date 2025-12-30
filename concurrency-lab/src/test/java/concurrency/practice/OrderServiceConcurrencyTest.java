package concurrency.practice;


import concurrency.practice.domain.Inventory;
import concurrency.practice.domain.ProductOrder;
import concurrency.practice.exception.PracticeException;
import concurrency.practice.repository.InventoryRepository;
import concurrency.practice.repository.ProductOrderRepository;
import concurrency.practice.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductOrderRepository productOrderRepository;

    @AfterEach
    void tearDown() {
        // 각 테스트 후 데이터베이스 정리
        productOrderRepository.deleteAllInBatch();
        inventoryRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("비관적 락: 100개의 재고를 가진 상품에 동시에 100개의 주문을 넣으면 재고가 0이 되어야 한다")
    void pessimistic_lock_test_for_inventory() throws InterruptedException {
        // Given: 재고가 100개인 상품 생성
        Inventory savedInventory = inventoryRepository.save(new Inventory("Test Product", 100));

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When: 100개의 스레드가 동시에 1개씩 주문 생성 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderService.createOrder(savedInventory.getId(), 1);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // 모든 스레드가 작업을 마칠 때까지 대기
        executorService.shutdown();

        // Then: 모든 주문이 성공하고, 재고는 0개가 되어야 한다.
        Inventory finalInventory = inventoryRepository.findById(savedInventory.getId()).orElseThrow();
        long totalOrders = productOrderRepository.count();

        assertThat(finalInventory.getStock()).isZero();
        assertThat(totalOrders).isEqualTo(100);
    }


    @Test
    @DisplayName("비관적 락: 1개의 재고를 가진 상품에 동시에 2개의 주문을 넣으면 하나는 실패해야 한다")
    void pessimistic_lock_fail_test_for_inventory() throws InterruptedException {
        // Given: 재고가 1개인 상품 생성
        Inventory savedInventory = inventoryRepository.save(new Inventory("Limited Product", 1));

        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // When: 2개의 스레드가 동시에 1개씩 주문 생성 시도
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderService.createOrder(savedInventory.getId(), 1);
                    successCount.incrementAndGet();
                } catch (PracticeException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // Then: 하나의 주문만 성공하고, 다른 하나는 재고 부족 예외가 발생해야 한다.
        Inventory finalInventory = inventoryRepository.findById(savedInventory.getId()).orElseThrow();
        long totalOrders = productOrderRepository.count();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
        assertThat(finalInventory.getStock()).isZero();
        assertThat(totalOrders).isEqualTo(1);
    }


    @Test
    @DisplayName("낙관적 락: 동일한 주문을 동시에 수정하면 한번은 성공하고 한번은 예외가 발생한다")
    void optimistic_lock_test_for_order_update() throws InterruptedException {
        // Given: 테스트용 상품 및 주문 데이터 생성
        Inventory savedInventory = inventoryRepository.save(new Inventory("Some Product", 10));
        ProductOrder savedOrder = productOrderRepository.save(new ProductOrder(savedInventory.getId(), 1));

        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);


        // When: 2개의 스레드가 동시에 같은 주문의 수량을 변경하려고 시도
        for (int i = 0; i < threadCount; i++) {
            int newQuantity = i + 2; // 스레드 1은 수량을 2로, 스레드 2는 3으로 변경 시도
            executorService.submit(() -> {
                try {
                    orderService.updateOrderQuantity(savedOrder.getId(), newQuantity);
                    successCount.incrementAndGet();
                } catch (PracticeException e) {
                    // OPTIMISTIC_LOCK_CONFLICT 예외가 발생한 경우 실패 카운트 증가
                    if (e.getErrorCode().name().equals("OPTIMISTIC_LOCK_CONFLICT")) {
                        failCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then: 한번의 수정은 성공하고, 다른 한번의 수정은 Optimistic Lock 예외를 받아 실패해야 한다.
        ProductOrder finalOrder = productOrderRepository.findById(savedOrder.getId()).orElseThrow();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);
        // 최종 수량은 2 또는 3 둘 중 하나가 된다. (먼저 성공한 스레드의 값)
        assertThat(finalOrder.getQuantity()).isIn(2, 3);
        // 버전은 1 증가해야 한다 (기본 0 -> 1)
        assertThat(finalOrder.getVersion()).isEqualTo(1);
    }
}
