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

#### 2.2.2.ConcurrentHashMap

ConcurrentHashMap是线程安全的

jdk 1.7中是引入了分段锁的概念

ConcurrentHashMap拥有16个Segment，Segment继承了ReentrantLock来保证线程安全

这样加锁只要在Segment的级别加锁即可

jdk1.8中去除了分段锁的概念，采用cas和synchronized操作保证线程安全