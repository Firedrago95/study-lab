package concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ConcurrencyTest {

    @Test
    @DisplayName("일반 LinkedList는 1:1 경합 상황에서 데이터 정합성이 깨진다")
    void linkedListFailureTest() throws InterruptedException {
        // given
        Queue<Object> queue = new LinkedList<>();
        int count = 10000;

        // when
        int result = runConcurrencyExperiment(queue, count);

        // then
        System.out.println("LinkedList 결과: " + result + "/" + count);
    }

    @Test
    @DisplayName("LinkedBlockingDeque는 소비자가 기다려주면 100% 정합성을 보장한다")
    void blockingDequeueSuccessTest() throws InterruptedException {
        // given
        Queue<Object> queue = new LinkedBlockingQueue<>();
        int count = 10000;

        // when
        int result = runConcurrencyExperiment(queue, count);

        // then
        System.out.println("blockingDequeue 결과: " + result + "/" + count);
        assertEquals(count, result, "BlockingQueue는 어떤 상황에서도 데이터 유실이 없어야 합니다.");
    }

    private int runConcurrencyExperiment(Queue<Object> queue, int count) throws InterruptedException {
        // 여러 스레드가 동시에 숫자를 올릴 때 데이터가 꼬이지 않도록 보장
        AtomicInteger processedCount = new AtomicInteger(0);
        // 두 스레드가 준비되자마다 동시에 출발하게 만드는 출발 신호기
        CountDownLatch latch = new CountDownLatch(2);
        // 스레드 풀을 만들어 작업을 직접 할당한다.
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Producer: 데이터 삽입
        Future<?> producerFuture = executor.submit(() -> {
            latch.countDown();
            try {
                latch.await();
            } catch (InterruptedException e) {
            }
            for (int i = 0; i < count; i++) {
                try {
                    queue.add(i);
                } catch (Exception e) {
                } // 에러 무시
            }
        });

        // Consumer: 데이터 추출
        executor.submit(() -> {
            latch.countDown();
            try { latch.await(); } catch (InterruptedException e) {}

            // 무작정 루프를 도는 게 아니라,
            // 목표 개수를 다 채울 때까지 혹은 생산자가 끝날 때까지 끈질기게 폴링합니다.
            while (processedCount.get() < count) {
                Object data = queue.poll();
                if (data != null) {
                    processedCount.incrementAndGet();
                } else {
                    // 생산자가 이미 끝났는데 큐도 비어있다면, 더 이상 나올 데이터가 없으므로 종료
                    if (producerFuture.isDone() && queue.isEmpty()) {
                        break;
                    }
                    // 큐가 잠시 비어있을 땐 CPU 과점 방지를 위해 아주 잠깐 쉽니다.
                    Thread.yield();
                }
            }
        });

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        return processedCount.get();
    }
}
