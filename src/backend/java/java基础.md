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

#### 2.2.2.ConcurrentHashMap

ConcurrentHashMap是线程安全的

jdk 1.7中是引入了分段锁的概念

ConcurrentHashMap拥有16个Segment，Segment继承了ReentrantLock来保证线程安全

这样加锁只要在Segment的级别加锁即可

扩容也是分段扩容，与HashMap差不多

jdk1.8中去除了分段锁的概念，采用cas，synchronized和LockSupport 操作保证线程安全

#### 2.2.3.TreeMap

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

3.3.Lock

### 3.4.线程池

## 4.JVM