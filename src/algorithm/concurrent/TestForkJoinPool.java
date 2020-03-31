package algorithm.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

public class TestForkJoinPool {

    private static Integer MAX = 200;

    static class MyForkJoinTask extends RecursiveTask<Integer> {

        // 子任务开始计算的值
        private Integer startValue;
        // 子任务结束计算的值
        private Integer endValue;

        public MyForkJoinTask(Integer startValue , Integer endValue) {
            this.startValue = startValue;
            this.endValue = endValue;
        }

        @Override
        protected Integer compute() {
            if (endValue - startValue <= MAX) {
                Integer totalValue = 0;
                for (int i = startValue; i <= endValue; i++) {
                    totalValue += i;
                }
                return totalValue;
            } else {
                MyForkJoinTask leftTask = new MyForkJoinTask(startValue, (startValue + endValue) / 2);
                leftTask.fork();
                MyForkJoinTask rightTask = new MyForkJoinTask((startValue + endValue) / 2 + 1, endValue);
                rightTask.fork();
                return leftTask.join() + rightTask.join();
            }
        }
    }

    public static void main(String[] args) {
        // 这是Fork/Join框架的线程池
        ForkJoinPool pool = new ForkJoinPool();
        ForkJoinTask<Integer> taskFuture = pool.submit(new MyForkJoinTask(1,1001));
        try {
            Integer result = taskFuture.get();
            System.out.println("result = " + result);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace(System.out);
        }
    }
}
