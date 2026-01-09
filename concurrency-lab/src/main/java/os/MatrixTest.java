package os;

public class MatrixTest {

    /*
    행 우선 탐색 소요시간: 40ms
    열 우선 탐색 소요시간: 337ms

    1. 왜 행(Row) 우선이 빠른가? (Spatial Locality)
    CPU가 메모리에서 데이터를 가져올 때는 딱 4바이트(int 하나)만 가져오지 않고,
    64바이트(Cache Line) 정도를 통째로 가져와서 L1 캐시에 올려둔다.
    matrix[0][0]을 읽으면, CPU는 "어차피 옆에 있는 [0][1], [0][2]도 읽겠지?" 하고 미리 캐시에 담는다.
    덕분에 다음 연산 때는 느린 RAM에 갈 필요가 없다.

    2. 왜 열(Column) 우선이 느린가? (Cache & TLB Hell)
    matrix[0][0]을 읽고 나서 바로 matrix[1][0]을 읽으려고 하면,
    메모리 주소상으로는 수만 바이트 뒤로 점프해야 한다.
    Cache Miss: 미리 가져온 64바이트 안에 데이터가 없다. 매번 RAM까지 다녀와야 한다.
    TLB Miss: 주소 변환 정보(지도)도 범위를 벗어난다.
    MMU가 다시 페이지 테이블을 뒤지러(Page Table Walk) RAM에 가야 한다.
     */
    public static void main(String[] args) {
        int size = 10000;
        int[][] matrix = new int[size][size];

        // [방법 1] 행 우선 탐색 (Row-major)
        // 메모리에 배치된 순서대로 옆집을 방문함 (Cache Line 활용 극대화)
        long start = System.currentTimeMillis();
        long sum1 = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                sum1 += matrix[i][j];
            }
        }
        System.out.println("행 우선 탐색 소요시간: " + (System.currentTimeMillis() - start) + "ms");

        // [방법 2] 열 우선 탐색 (Column-major)
        // 한 칸 읽을 때마다 수만 칸 뒤로 점프함 (Cache/TLB Miss 유발)
        start = System.currentTimeMillis();
        long sum2 = 0;
        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                sum2 += matrix[i][j];
            }
        }
        System.out.println("열 우선 탐색 소요시간: " + (System.currentTimeMillis() - start) + "ms");

        // 결과 비교용 (의미 없는 출력 방지)
        if (sum1 != sum2) {
            System.out.println("결과가 다름!");
        }
    }
}
