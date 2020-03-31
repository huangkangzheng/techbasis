package algorithm.concurrent;

import java.util.concurrent.CountDownLatch;

class Foo {

    private CountDownLatch letch1 = new CountDownLatch(1);
    private CountDownLatch letch2 = new CountDownLatch(1);

    public Foo() {

    }

    public void first(Runnable printFirst) throws InterruptedException {

        // printFirst.run() outputs "_1_first". Do not change or remove this line.
        printFirst.run();
        letch1.countDown();
    }

    public void second(Runnable printSecond) throws InterruptedException {
        letch1.await();
        // printSecond.run() outputs "second". Do not change or remove this line.
        printSecond.run();
        letch2.countDown();
    }

    public void third(Runnable printThird) throws InterruptedException {
        letch2.await();
        // printThird.run() outputs "third". Do not change or remove this line.
        printThird.run();
    }
}
