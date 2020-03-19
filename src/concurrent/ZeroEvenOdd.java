package concurrent;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntConsumer;

class ZeroEvenOdd {
    private int n;
    private Semaphore zero = new Semaphore(1);
    private Semaphore odd = new Semaphore(0);
    private Semaphore even = new Semaphore(0);

    public ZeroEvenOdd(int n) {
        this.n = n;
    }

    // printNumber.accept(x) outputs "x", where x is an integer.
    public void zero(IntConsumer printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i++) {
            zero.acquire();
            printNumber.accept(0);
            if ((i & 1) == 0) {
                even.release();
            } else {
                odd.release();
            }
        }
    }

    public void even(IntConsumer printNumber) throws InterruptedException {
        for (int i = 2; i <= n; i += 2) {
            even.acquire();
            printNumber.accept(i);
            zero.release();
        }
    }

    public void odd(IntConsumer printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i += 2) {
            odd.acquire();
            printNumber.accept(i);
            zero.release();
        }
    }
}

class ZeroEvenOddWithLock {
    private int n;
    private Lock lock = new ReentrantLock();
    private Condition zero = lock.newCondition();
    private Condition odd = lock.newCondition();
    private Condition even = lock.newCondition();
    private boolean flag = false;

    public ZeroEvenOddWithLock(int n) {
        this.n = n;
    }

    // printNumber.accept(x) outputs "x", where x is an integer.
    public void zero(IntConsumer printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i++) {
            lock.lock();
            while (flag == true) {
                zero.await();
            }
            printNumber.accept(0);
            if ((i & 1) == 0) {
                even.signal();
            } else {
                odd.signal();
            }
            flag = true;
            lock.unlock();
        }
    }

    public void even(IntConsumer printNumber) throws InterruptedException {
        for (int i = 2; i <= n; i += 2) {
            lock.lock();
            while (flag == false) {
                even.await();
            }
            printNumber.accept(i);
            zero.signal();
            flag = false;
            lock.unlock();
        }
    }

    public void odd(IntConsumer printNumber) throws InterruptedException {
        for (int i = 1; i <= n; i += 2) {
            lock.lock();
            while (flag == false) {
                odd.await();
            }
            printNumber.accept(i);
            zero.signal();
            flag = false;
            lock.unlock();
        }
    }

    public static void main(String[] args) {
        ZeroEvenOddWithLock z = new ZeroEvenOddWithLock(5);
        new Thread(() -> {
            try {
                z.zero(System.out::print);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                z.odd(System.out::print);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                z.even(System.out::print);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
