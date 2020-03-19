package concurrent;

import java.util.function.IntConsumer;

class FizzBuzz {
    private int n;
    private volatile int latch = 1;

    public FizzBuzz(int n) {
        this.n = n;
    }

    // printFizz.run() outputs "fizz".
    public void fizz(Runnable printFizz) throws InterruptedException {
        while (latch <= n) {
            synchronized (this) {
                if (latch > n) break;
                if (latch % 3 == 0 && latch % 5 != 0) {
                    printFizz.run();
                    latch++;
                }
            }
        }
    }

    // printBuzz.run() outputs "buzz".
    public void buzz(Runnable printBuzz) throws InterruptedException {
        while (latch <= n) {
            synchronized (this) {
                if (latch > n) break;
                if (latch % 3 != 0 && latch % 5 == 0) {
                    printBuzz.run();
                    latch++;
                }
            }
        }
    }

    // printFizzBuzz.run() outputs "fizzbuzz".
    public void fizzbuzz(Runnable printFizzBuzz) throws InterruptedException {
        while (latch <= n) {
            synchronized (this) {
                if (latch > n) break;
                if (latch % 3 == 0 && latch % 5 == 0) {
                    printFizzBuzz.run();
                    latch++;
                }
            }
        }
    }

    // printNumber.accept(x) outputs "x", where x is an integer.
    public void number(IntConsumer printNumber) throws InterruptedException {
        while (latch <= n) {
            synchronized (this) {
                if (latch > n) break;
                if (latch % 3 != 0 && latch % 5 != 0) {
                    printNumber.accept(latch);
                    latch++;
                }
            }
        }
    }
}