package algorithm._2018._1_first;

/*
 * 动态规划
 * https://blog.csdn.net/dongmuyang/article/details/91356454
 */
public class _5_SuperEggDrop {
    public int superEggDrop(int K, int N) {
        int drop[][] = new int[K + 1][N + 1];
        for (int j = 1; j <= N; j++) {
            drop[1][j] = j;
        }
        for (int i = 2; i <= K; i++) {
            for (int j = 1; j <= N; j++) {
                int tMin = N;
                for (int x = 1; x <= j; x++) {
                    tMin = Math.min(tMin, 1 + Math.max(drop[i - 1][x - 1], drop[i][j - x]));
                }
                drop[i][j] = tMin;
            }
        }
        return drop[K][N];
    }
}
