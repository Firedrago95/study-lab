package os;

import java.util.concurrent.atomic.AtomicLong;

public class ContextSwitchTest {

    /*
    10_000_000번 부동소수점 연산 실험 결과
    단일 스레드 소요시간: 561ms
    8개 스레드 소요 시간: 18ms

    10000000번 부동소수점 연산 실험 결과
    단일 스레드 소요시간: 553ms
    50000개 스레드 소요 시간: 1386ms (스레드 컨텍스트 스위칭 비용 증가)
     */
    public static void main(String[] args) throws InterruptedException {
        long totalIncrements = 10_000_000L; // 10억
        System.out.println(totalIncrements + "번 부동소수점 연산 실험 결과");

        // 1. 단일 스레드 작업
        long start = System.currentTimeMillis();
        long localSum1 = 0;
        for (int i = 0; i < totalIncrements; i++) {
            // 1. 부동소수점 복합 연산 (CPU 부하 최적)
            double val = Math.sin(i) * Math.cos(i) + Math.sqrt(i);

            // 2. 결과값을 localSum에 반영 (최적화로 코드가 삭제되는 것 방지)
            // 연산 결과가 계속 변하므로 JIT 컴파일러가 이 루프를 마음대로 삭제하지 못합니다.
            localSum1 += (long) val;
        }
        System.out.println("단일 스레드 소요시간: " + (System.currentTimeMillis() - start) + "ms");

        // 2. 수많은 스레드가 나눠서 작업 (8개 스레드가 분담)
        int threadCount = 50000;
        Thread[] threads = new Thread[threadCount];
        long incrementsPerThread = totalIncrements / threadCount;

        start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                long localSum = 0;
                for (long j = 0; j < incrementsPerThread; j++) {
                    // 단일 스레드와 '완벽하게 동일한' 부하 연산 수행
                    double val = Math.sin(j) * Math.cos(j) + Math.sqrt(j);
                    localSum += (long) val;
                }
                // JIT 최적화 방지용 (계산 결과를 무의미하지 않게 만듦)
                if (localSum == -1) System.out.println(localSum);
            });
            threads[i].start();
        }

        for (Thread t : threads) t.join();
        System.out.println(threadCount + "개 스레드 소요 시간: " + (System.currentTimeMillis() - start) + "ms");
    }
}
