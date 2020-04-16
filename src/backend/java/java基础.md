[TOC]

## 1.基础知识

（1）8种基本数据类型 int, short, long, char, float, double, byte, boolean

（2）封装，继承，多态

（3）hashCode，equals，覆写equals必须要覆写hashCode

（4）toString

（5）类加载器

​        BootstrapClassLoader（rt.jar），ExtensionClassLoader(jdk中的其他jar包)，APPClassLoader（classpath下的jar包） 

​        类加载机制（双亲委托）：自底向上搜索，自顶向下加载，避免类的重复加载

（6）异常

​        分为Exception和Error，Exception分为检查性异常（如IOException）和非检查性异常（如ArrayIndexOutOfBoundsException）

​        编译时异常（编译时报错，如ClassNotFoundException）

​        运行时异常（平常大多数都是遇到这个异常，NullPointException，IndexOutOfBoundException等等）

​        Error（JVM不可处理或者不可恢复的情况，如OutOfMemoryError，NoClassDefFoundError）

## 2.集合类（Collection和Map）

### 2.1.Collection

collection下的是List，Set

List有序，Set不重复无序

List和Set都是线程不安全

Collection遍历移除元素要用迭代器iterator，否则会有数组下标异常的问题

#### 2.1.1.List

ArrayList和LinkedList的区别：实际上就是数组和链表的优缺点，数组无法扩容，但是查找快，用下表，链表扩容方便，但是查找慢，必须得遍历

ArrayList是1.5倍扩容：int newCapacity = oldCapacity + (oldCapacity >> 1);

#### 2.1.2.Set

Set的底层实现是Map，理解Set重点先理解Map

HashSet的底层实现是HashMap，HashSet实际上就是HashMap.key（不重复）

TreeSet是有序无重队列，底层实现是TreeMap

#### 2.1.3.Queue

Quque：FIFO

Dequeue：双向队列（双向链表）

线程安全的Queue：LinkedBlockingQueue和ArrayBlockingQueue（参考3.5线程池）

### 2.2.Map

#### 2.2.1.HashMap

HashMap的数据结构是数组+链表（链表长度大于8转化成红黑树）Node<K, V>[] table

put(K，V)：如果不存在则返回null，如果存在则替换为新的V并返回原来的oldV

putIfAbsent(K，V)：如果不存在则放入值并返回null，存在则直接返回原来值oldV不进行替换

put逻辑：

​        首先根据K.hashCode算出hash值：hash = K.hashCode() ^ (h >>> 16)

​        根据hash值算出K应该所处的数据下标index：index  = (n - 1) & hash

​        如果table[index] == null，则K就放在table[index]位置

​        如果table[index] != null，则会遍历链表在最后面链上

​        如果链表长度大于TREEIFY_THRESHOLD（默认为8）就会转换成红黑树

​        默认的数组长度是16，有个平衡因子DEFAULT_LOAD_FACTOR默认值为0.75，也就是说，当数组中的容量达到length * 0.75，就要触发resize（），这是由于如果不做扩容的话，接下来的hash碰撞就会更频繁，这样链表越拉越长，搜索起来就更慢

​        resize()方法每次是2倍扩容，这是由于算下标index的算法为index  = (length - 1) & hash，默认是2的4次方16，

​        2倍扩容的话，length - 1永远是2的n次方 - 1，转换成2进制就是所有的位上都是1，hash与其做&运算，可以更均匀的分散在数组上，且&运算效率很高

​        扩容之后，假设原来length是16，hash值为1，17，都是index1上，扩容之后length为32，hash值为1还在index1上，但是hash值17会在index17上，均匀的分散在了两边，减少了hash碰撞且各自之后的链表长度也较短，都是提高了搜索效率

HashMap是线程不安全的，主要是由于resize()时候线程不安全

jdk1.8之前采用头插法（新数据放在链表靠前的地方），由于认为新插入的值可能比较容易被用到，放前面搜索快

jdk1.8之后采用尾插法，由于引入了红黑树加快搜索效率

LinkedHashMap实现了**LRU（Least Recently Used 最近最少使用）缓存**的功能，每次放入元素，都会动态调整链表将最近的元素放在靠前的位置

#### 2.2.2.ConcurrentHashMap

ConcurrentHashMap是线程安全的

jdk 1.7中是引入了分段锁的概念

ConcurrentHashMap拥有16个Segment，Segment继承了ReentrantLock来保证线程安全

这样加锁只要在Segment的级别加锁即可

扩容也是分段扩容，与HashMap差不多

jdk1.8中去除了分段锁的概念，采用cas，synchronized和LockSupport 操作保证线程安全

#### 2.2.3.TreeMap

TreeMap底层是红黑树，用于排序

#### 2.2.4.红黑树

## 3.多线程

java的每个对象都各自拥有一个锁

锁池：竞争锁的线程会进入锁池

等待池：释放锁（比如wait）会进入等待池，知道被唤醒（notify）才会重新进入锁池去竞争锁

wait()：在某种条件下线程被阻塞 （会释放锁，需要被唤醒） while(condition) { object.wait(); } object.notifyAll

notify()：唤醒1个被wait()阻塞的线程

notifyAll()：唤醒所有被wait阻塞住的线程

join()：主线程thread1调用其他线程thread2.join()，主线程需要等待thread2执行完毕才接着执行主线程的内容，thread1 -> thread2.join() -> 继续thread1

yield()：thread1.yield()会使当前线程释放cpu资源重回就绪态（不会释放锁）

可重入锁：同一个线程，获得锁了之后可以再次获得锁，利用计数器count++，避免死锁（synchronized和reentrantLock）

不可重入锁：同一个线程获得锁之后必须释放锁才能再次获得锁

volatile：禁止工作内存拷贝主存（线程可见性，读取只从主存中直接读取，参考3.4），禁止指令重排序优化，适用于1个线程写，多个线程读读的场景

#### 3.1.CAS

悲观锁：每次都要先得到锁才能进入同步代码块，如Synchronized

乐观锁：假定实际值和我的预期值相等我就允许其进行更新同步变量，如CAS（自旋锁）

CAS（compare and swap）：假定CAS（actualValue, exceptValue, newValue），当实际的值和期望值相等时才将newValue赋值返回true，否则返回false  while(! cas(actualValue, exceptValue, newValue)) {不成功的处理操作}，如AtomicInteger等等

CAS不能解决的问题：ABA问题，线程1更新为B，线程2又更新为A，此时仍然是A，允许被更新，如果是计数器不影响，但某些情况下不允许这种情况发生，所以引入AtomicStampedReference，加入了版本号概念，当版本相同且实际值等于期望值才更新成功

AtomicReference：CAS只能保证一个原子类型变量，利用对象整合多个原子类型变量即AtomicReference

### 3.2.Synchronized

锁实例对象（instance）：非静态方法，锁（this）等等都是锁实例对象

锁类对象（class）：静态方法，静态变量，锁（class）等等都是锁类对象

锁的核心思想是：多个线程竞争共享资源（如jvm中，多个线程，竞争堆内存中的某个对象，需要加锁以保证线程安全）

Synchronized的实现：每个对象都拥有一个monitor（监视器||锁），同一时刻只有一个线程可以获得monitor，从而进入到同步代码块，即互斥性（排它性）

对象头（Mark Word）：monitor实际上是类似于对对象的一个标志，那么这个标志就是存放在Java对象的对象头，32位JVM Mark Word默认存储结构为

|  锁状态  |     25bit      |     4bit     | 1bit是否为偏向锁 | 2bit锁标志位 |
| :------: | :------------: | :----------: | :--------------: | :----------: |
| 无锁状态 | 对象的hashcode | 对象分代年龄 |        0         |      01      |

4种锁状态：从低到高分别是**无锁状态、偏向锁状态、轻量级锁状态和重量级锁状态**，这几个状态会随着竞争情况逐渐升级，**锁可以升级但不能降级**，对应的Mark Word如下所示：

![](..\..\resources\java基础\各类锁markword.jpg)

偏向锁：大多数情况下，锁不仅不存在多线程竞争，而且总是由同一线程多次获得，为了让线程获得锁的代价更低而引入了偏向锁 

​        当一个线程访问同步块并获取锁时，会在**对象头**和**栈帧中的锁记录**里存储锁偏向的线程ID，以后该线程在进入和退出同步块时不需要进行CAS操作来加锁和解锁，只需简单地测试一下对象头的Mark Word里是否存储着指向当前线程的偏向锁。如果测试成功，表示线程已经获得了锁。如果测试失败，则需要再测试一下Mark Word中偏向锁的标识是否设置成1（表示当前是偏向锁）：如果没有设置，则使用CAS竞争锁；如果设置了，则尝试使用CAS将对象头的偏向锁指向当前线程

![](..\..\resources\java基础\偏向锁.jpg)

​        偏向锁在Java 6和Java 7里是默认启用的，但是它在应用程序启动几秒钟之后才激活，如有必要可以使用JVM参数来关闭延迟：**-XX:BiasedLockingStartupDelay=0**。如果你确定应用程序里所有的锁通常情况下处于竞争状态，可以通过JVM参数关闭偏向锁：**-XX:-UseBiasedLocking=false**，那么程序默认会进入轻量级锁状态

轻量锁：

**加锁**

线程在执行同步块之前，JVM会先在当前线程的栈桢中**创建用于存储锁记录的空间**，并将对象头中的Mark Word复制到锁记录中，官方称为**Displaced Mark Word**。然后线程尝试使用CAS**将对象头中的Mark Word替换为指向锁记录的指针**。如果成功，当前线程获得锁，如果失败，表示其他线程竞争锁，当前线程便尝试使用自旋来获取锁

**解锁**

轻量级解锁时，会使用原子的CAS操作将Displaced Mark Word替换回到对象头，如果成功，则表示没有竞争发生。如果失败，表示当前锁存在竞争，锁就会膨胀成重量级锁

![](..\..\resources\java基础\轻量锁.jpg)

重量锁：每次执行同步代码块都需要竞争锁

各种锁的比较：

![](..\..\resources\java基础\各种锁的比较.jpg)

### 3.3.Lock

ReentrantLock：

Condition：

公平锁：按照FIFO的规则获取锁

非公平锁：先尝试抢占锁，如果被其他线程占用再进入队列

### 3.4.happens-before原则

多个线程对于**共享资源**（如堆区中的对象）的访问是线程不安全的

JVM的工作机制：每个线程拥有自己的线程栈和工作内存，共享资源存放在主存中，每次线程访问共享资源就是把主存中的变量拷贝到工作内存中，之后的读写操作都是在工作内存中，最后将工作内存的变量副本写回到主存中去，JMM定义了这种工作方式（volatile就是禁止线程复制共享资源到工作内存，所有线程都是直接从主存读，故而主存的修改所有线程都是立马可见的）

重排序：在执行程序时，**为了提高性能，编译器和处理器常常会对指令进行重排序** 

编译器优化的重排序：编译器在不改变单线程程序语义的前提下，可以重新安排语句的执行顺序

指令级并行的重排序：现代处理器采用了指令级并行技术来将多条指令重叠执行。如果**不存在数据依赖性**，处理器可以改变语句对应机器指令的执行顺序

内存系统的重排序：由于处理器使用缓存和读/写缓冲区，这使得加载和存储操作看上去可能是在乱序执行的

**happens-before原则：**

程序顺序规则：一个线程中的每个操作，happens-before于该线程中的任意后续操作。

监视器锁规则：对一个锁的解锁，happens-before于随后对这个锁的加锁。

volatile变量规则：对一个volatile域的写，happens-before于任意后续对这个volatile域的读。

传递性：如果A happens-before B，且B happens-before C，那么A happens-before C。

start()规则：如果线程A执行操作ThreadB.start()（启动线程B），那么A线程的ThreadB.start()操作happens-before于线程B中的任意操作。

join()规则：如果线程A执行操作ThreadB.join()并成功返回，那么线程B中的任意操作happens-before于线程A从ThreadB.join()操作成功返回。

程序中断规则：对线程interrupted()方法的调用先行于被中断线程的代码检测到中断时间的发生。

对象finalize规则：一个对象的初始化完成（构造函数执行结束）先行于发生它的finalize()方法的开始

### 3.5.线程池

池化思想，避免线程重复创建销毁

ThreadPool（corePoolSize, maxPoolSize, QueueCapacity, KeepAliveSeconds, rejectHandler）

corePool大小满了，进入队列排队，队列满了扩大到maxPoolSize，仍旧不足则执行rejectHandler

corePoolSize：核心池大小

maxPoolSize：线程池最大大小

QueueCapacity：队列长度

RejectHandler：拒绝策略，有abortPolicy（丢弃任务并抛异常），DiscardPolicy（丢弃任务但不抛异常），DiscardOldestPolicy（丢弃队列最前面的任务然后重新提交被拒绝的任务），CallerRunsPolicy（由调用线程执行该任务）

KeepAliveSeconds：超过核心池大小（corePoolSize < x < maxPoolSize）之后新增的线程，空闲keepAliveSeconds之后就会被关闭

LinkedBlockingQueue：

​        LinkedBlockingQueue中的锁是分离的，生产者的锁PutLock，消费者的锁takeLock

​        LinkedBlockingQueue内部维护的是一个链表结构，在生产和消费的时候，需要创建Node对象进行插入或移除，大批量数据的系统中，其对于GC的压力会比较大 

​        LinkedBlockingQueue有默认的容量大小为：Integer.MAX_VALUE，当然也可以传入指定的容量大小

​	LinkedBlockingQueue中使用了一个AtomicInteger对象来统计元素的个数

ArrayBlockingQueue：

​        ArrayBlockingQueue生产者和消费者使用的是同一把锁

​        ArrayBlockingQueue内部维护了一个数组 ，在生产和消费的时候，是直接将枚举对象插入或移除的，不会产生或销毁任何额外的对象实例

　　ArrayBlockingQueue在初始化的时候，必须传入一个容量大小的值

 　   ArrayBlockingQueue则使用int类型来统计元素 

### 3.6.Samphore，CountdownLatch，CyclicBarrier，ForkJoinPool

Samphore：信号量，acquire()获取信号量（i--，大于等于0则获得成功），release()释放信号量（i++）

CountdownLatch：计数器，比如初始化为5，主线程调用latch.await()，则会阻塞在这里直到计数器到0的时候才会接着执行，latch.countdown()即计数器减1，无法重复使用

CyclicBarrier：循环栅栏，比如初始话为5，调用barrier.await()表示线程已经执行到了这里，当5个线程都执行到了这里则会冲破栅栏继续执行，以此类推，可重复使用，适用于多线程计算任务，当多个任务同时达到栅栏处然后接着执行后续工作

ForkJoinPool（暂未深入）：分而治之的思想

```java
public class SumTask extends RecursiveTask<Integer> {

    private Integer start = 0;
    private Integer end = 0;

    public SumTask(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute() {

        if (end - start < 100) {
            //小于100时直接返回结果
            int sumResult = 0;
            for (int i = start; i <= end; i++) {
                sumResult += i;
            }
            return sumResult;
        } else {
            //大于一百时进行分割
            int middle = (end + start) / 2;
            SumTask leftSum = new SumTask(this.start, middle);
            SumTask rightSum = new SumTask(middle, this.end);
            leftSum.fork();
            rightSum.fork();
            return leftSum.join() + rightSum.join();
        }
    }
} 

public static void main(String[] args) {
        ForkJoinPool forkJoinPool=new ForkJoinPool(100);
        SumTask sumTask = new SumTask(1, 999999);
        forkJoinPool.submit(sumTask);
        System.out.print("result:" + sumTask.join());
    }
```

核心思想是每个线程都有各自的workQueue，task每次是被提交到线程本地的workqueue

fork()方法：当前线程不是个`ForkJoinWorkerThread`的时候，则加入到`ForkJoinPool`线程池(基于`ExecutorService`实现);
 如果当前线程已经是个`ForkJoinWorkerThread`了，则把这个任务加入到当前线程的`workQueue`;

执行task的逻辑：

a. 线程以`LIFO`先进后出方式从本地队列获取任务，执行，直到自己的队列为空；
 b. 查看其他`ForkJoinWorkerThread`是否有未执行`task`，有的话通过`work−stealing`窃取,窃取方式为`FIFO`先进先出，减少竞争；优先看曾今从自己那里窃取任务的thread，如果有的话；
 c. 任务运行完成时，返回结果

## 4.JVM

JVM内存模型

![](..\..\resources\java基础\java内存模型.jpg)

