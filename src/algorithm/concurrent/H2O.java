package algorithm.concurrent;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

class H2O {

    private CyclicBarrier barrier = new CyclicBarrier(3);
    private Semaphore o = new Semaphore(1);
    private Semaphore h2 = new Semaphore(2);

    public H2O() {

    }

    public void hydrogen(Runnable releaseHydrogen) throws InterruptedException {
        h2.acquire();
        try {
            barrier.await();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
        // releaseHydrogen.run() outputs "H". Do not change or remove this line.
        releaseHydrogen.run();
        h2.release();
    }

    public void oxygen(Runnable releaseOxygen) throws InterruptedException {
        o.acquire();
        try {
            barrier.await();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
        // releaseOxygen.run() outputs "O". Do not change or remove this line.
        releaseOxygen.run();
        o.release();
    }
}