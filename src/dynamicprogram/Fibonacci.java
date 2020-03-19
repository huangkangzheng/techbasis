package dynamicprogram;

public class Fibonacci {
    public int solutionFibonacci(int n) {
        if (n == 0)
            return 0;
        if (n == 1)
            return 1;
        int[] result = new int[n + 1];
        result[0] = 0;
        result[1] = 1;
        for (int i = 2; i <= n; i++) {
            result[i] = result[i - 1] + result[i - 2];
        }
        return result[n];
    }
}
