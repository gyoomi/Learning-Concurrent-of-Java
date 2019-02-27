## 并发容器之ConcurrentHashMap源码实现
### 一、背景
其实这一小节主要内容可以换一种说法："为什么要使用ConcurrentHashMap,而非HashMap?尤其是在多线程环境下？"
其实我简单总结了以下三点原因，可以回答上述问题。此外声明下本文研究的源码版本以JDK1.7标准，其他版本的代码略有差异，尤其是JDK1.8
,后面有时间在详细研究。

1. **HashMap的线程不安全**

   <font color='red'>在多线程环境下，使用HashMap进行put操作会引起死循环，导致CPU利用率接近100%，所
   以在并发情况下不能使用HashMap。</font>

   <font color='red'>HashMap在并发执行put操作时会引起死循环，是因为多线程会导致HashMap的Entry链表
   形成环形数据结构，一旦形成环形数据结构，Entry的next节点永远不为空，就会产生死循环获
   取Entry。</font>


2. **效率低下的HashTable**

   <font color='red'>HashTable容器使用synchronized来保证线程安全，但在线程竞争激烈的情况下HashTable
   的效率非常低下。因为当一个线程访问HashTable的同步方法，其他线程也访问HashTable的同
   步方法时，会进入阻塞或轮询状态。如线程1使用put进行元素添加，线程2不但不能使用put方
   法添加元素，也不能使用get方法来获取元素，所以竞争越激烈效率越低。</font>

3. **ConcurrentHashMap的锁分段技术可有效提升并发访问率**

   HashTable容器在竞争激烈的并发环境下表现出效率低下的原因是所有访问HashTable的
   线程都必须竞争同一把锁，假如容器里有多把锁，每一把锁用于锁容器其中一部分数据，那么
   当多线程访问容器里不同数据段的数据时，线程间就不会存在锁竞争，从而可以有效提高并
   发访问效率，这就是ConcurrentHashMap所使用的锁分段技术。首先<font color='red'>将数据分成一段一段地存
   储，然后给每一段数据配一把锁，当一个线程占用锁访问其中一个段数据的时候，其他段的数
   据也能被其他线程访问。</font>
### 二、ConcurrentHashMap实现原理
#### 2.1 原理概述
正如第一节中描述的3个理由，其实其中已经包括了其原理描述。
HashTable容器在竞争激烈的并发环境下表现出效率低下的原因是所有访问HashTable的
线程都必须竞争同一把锁，假如容器里有多把锁，每一把锁用于锁容器其中一部分数据，那么
当多线程访问容器里不同数据段的数据时，线程间就不会存在锁竞争，从而可以有效提高并
发访问效率，这就是ConcurrentHashMap所使用的锁分段技术。首先<font color='red'>将数据分成一段一段地存
储，然后给每一段数据配一把锁，当一个线程占用锁访问其中一个段数据的时候，其他段的数
据也能被其他线程访问。</font>

暂时先这样理解，下面在解析ConcurrentHashMap的属性、构造器及内部类的时候，你就会明白上面的那段话。
#### 2.2 原理图

![](./asserts/001.png)

记住这张图的样子，在后面的操作的逻辑可以大概联想这张图进行思考，应该会顺畅很多
### 三、源码分析
#### 3.1 属性
```
    /* ---------------- Constants:一些系统默认的常量 -------------- */
    
    // 默认初始容量大小
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    // 默认负载因子
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    // 默认的segment容量大小
    static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    // 最大容量
    static final int MAXIMUM_CAPACITY = 1 << 30;

    // 最小segment容量大小
    static final int MIN_SEGMENT_TABLE_CAPACITY = 2;

    // 最小segment容量大小。也就是65535
    static final int MAX_SEGMENTS = 1 << 16; // slightly conservative

    // 在锁定之前重试的次数（内部类HashEntry中使用）
    static final int RETRIES_BEFORE_LOCK = 2;
    
    /* ---------------- Fields:属性 -------------- */
    private transient final int hashSeed = randomHashSeed(this);
    
    // segmentMask是散列运算的掩码，等于ssize减1
    // 掩码的二进制各个位的值都是1。
    final int segmentMask;

    // segmentShift用于定位参与散列运算的位数
    final int segmentShift;

    // segments数组
    final Segment<K,V>[] segments;
    
    // key集合
    transient Set<K> keySet;
    // 实体集合
    transient Set<Map.Entry<K,V>> entrySet;
    // value集合
    transient Collection<V> values;
```
#### 3.2 构造器
```
    // 思路：通过initialCapacity、loadFactor和concurrencyLevel等几个
    //       参数来初始化segment数组、段偏移量segmentShift、段掩码segmentMask和每个segment里的
    //       HashEntry数组。
    // 步骤：
    // 1. 初始化一些属性值
    // 1.1 计算segments数组的长度ssize的值。通过concurrencyLevel计算得出的。为了能
    //     通过按位与的散列算法来定位segments数组的索引，必须保证segments数组的长度是2的N次方
    //     （power-of-two size），所以必须计算出一个大于或等于concurrencyLevel的最小的2的N次方值
    //     来作为segments数组的长度。假如concurrencyLevel等于14、15或16，ssize都会等于16，即容器里锁的个数也是16。
    //     concurrencyLevel的最大值是65535，这意味着segments数组的长度最大为65536，对应的二进制是16位。
    // 1.2 初始化segmentShift和segmentMask。
    //     这两个全局变量需要在定位segment时的散列算法里使用，sshift等于ssize从1向左移位的
    //     次数，在默认情况下concurrencyLevel等于16，1需要向左移位移动4次，所以sshift等于4。
    //     segmentShift用于定位参与散列运算的位数，segmentShift等于32减sshift，所以等于28，这里之所
    //     以用32是因为ConcurrentHashMap里的hash()方法输出的最大数是32位的，后面的测试中我们
    //     可以看到这点。segmentMask是散列运算的掩码，等于ssize减1，即15，掩码的二进制各个位的
    //     值都是1。因为ssize的最大长度是65536，所以segmentShift最大值是16，segmentMask最大值是
    //     65535，对应的二进制是16位，每个位都是1。
    // 2. 初始化Segment[]对象
    // 2.1 初始化Segment对象
    //     输入参数initialCapacity是ConcurrentHashMap的初始化容量，loadfactor是每个segment的负
    //     载因子，在构造方法里需要通过这两个参数来初始化数组中的每个segment。
    //     变量cap就是segment里HashEntry数组的长度，它等于initialCapacity除以ssize
    //     的倍数c，如果c大于1，就会取大于等于c的2的N次方值，所以cap不是1，就是2的N次方。
    //     segment的容量threshold＝（int）cap*loadFactor，默认情况下initialCapacity等于16，loadfactor等于
    //     0.75，通过运算cap等于1，threshold等于零。
    // 2.2 初始化Segment[]对象
    public ConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (concurrencyLevel > MAX_SEGMENTS)
            concurrencyLevel = MAX_SEGMENTS;
        // Find power-of-two sizes best matching arguments
        int sshift = 0;
        int ssize = 1;
        while (ssize < concurrencyLevel) {
            ++sshift;
            ssize <<= 1;
        }
        this.segmentShift = 32 - sshift;
        this.segmentMask = ssize - 1;
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        int c = initialCapacity / ssize;
        if (c * ssize < initialCapacity)
            ++c;
        int cap = MIN_SEGMENT_TABLE_CAPACITY;
        while (cap < c)
            cap <<= 1;
        // create segments and segments[0]
        Segment<K,V> s0 =
            new Segment<K,V>(loadFactor, (int)(cap * loadFactor),
                             (HashEntry<K,V>[])new HashEntry[cap]);
        Segment<K,V>[] ss = (Segment<K,V>[])new Segment[ssize];
        UNSAFE.putOrderedObject(ss, SBASE, s0); // ordered write of segments[0]
        this.segments = ss;
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL);
    }

    public ConcurrentHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }

    public ConcurrentHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
    }

    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                      DEFAULT_INITIAL_CAPACITY),
             DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL);
        putAll(m);
    }
```
#### 3.3 定位Segment
既然ConcurrentHashMap使用分段锁Segment来保护不同段的数据，那么在插入和获取元素
的时候，必须先通过散列算法定位到Segment。可以看到ConcurrentHashMap会首先使用
Wang/Jenkins hash的变种算法对元素的hashCode进行一次再散列。

```
    private int hash(Object k) {
        int h = hashSeed;

        if ((0 != h) && (k instanceof String)) {
            return sun.misc.Hashing.stringHash32((String) k);
        }

        h ^= k.hashCode();

        // Spread bits to regularize both segment and index locations,
        // using variant of single-word Wang/Jenkins hash.
        h += (h <<  15) ^ 0xffffcd7d;
        h ^= (h >>> 10);
        h += (h <<   3);
        h ^= (h >>>  6);
        h += (h <<   2) + (h << 14);
        return h ^ (h >>> 16);
    }
```

<font color='red'>之所以进行再散列，目的是减少散列冲突，使元素能够均匀地分布在不同的Segment上，
从而提高容器的存取效率。假如散列的质量差到极点，那么所有的元素都在一个Segment中，
不仅存取元素缓慢，分段锁也会失去意义。笔者做了一个测试，不通过再散列而直接执行散列
计算。</font>

```
System.out.println(Integer.parseInt("0001111", 2) & 15);
System.out.println(Integer.parseInt("0011111", 2) & 15);
System.out.println(Integer.parseInt("0111111", 2) & 15);
System.out.println(Integer.parseInt("1111111", 2) & 15);
```

计算后输出的散列值全是15，通过这个例子可以发现，如果不进行再散列，散列冲突会非
常严重，因为只要低位一样，无论高位是什么数，其散列值总是一样。我们再把上面的二进制
数据进行再散列后结果如下（为了方便阅读，不足32位的高位补了0，每隔4位用竖线分割下）。

```
0100｜0111｜0110｜0111｜1101｜1010｜0100｜1110
1111｜0111｜0100｜0011｜0000｜0001｜1011｜1000
0111｜0111｜0110｜1001｜0100｜0110｜0011｜1110
1000｜0011｜0000｜0000｜1100｜1000｜0001｜1010
```

可以发现，每一位的数据都散列开了，通过这种再散列能让数字的每一位都参加到散列
运算当中，从而减少散列冲突。ConcurrentHashMap通过以下散列算法定位segment。

```
    private Segment<K,V> segmentForHash(int h) {
        long u = (((h >>> segmentShift) & segmentMask) << SSHIFT) + SBASE;
        return (Segment<K,V>) UNSAFE.getObjectVolatile(segments, u);
    }
```

默认情况下segmentShift为28，segmentMask为15，再散列后的数最大是32位二进制数据，
向右无符号移动28位，意思是让高4位参与到散列运算中，（hash>>>segmentShift）
&segmentMask的运算结果分别是4、15、7和8，可以看到散列值没有发生冲突。
#### 3.4 核心内部类-Segment
<font color='red'>Segment类是一类特殊的hash表，继承了ReentrantLock类具备了锁的功能。这是ConcurrentHashMap中最核心的内部类，put，get, remove的核心逻辑都在其中。</font>

```
static final class Segment<K,V> extends ReentrantLock implements Serializable {

        private static final long serialVersionUID = 2249069246763182397L;

        /**
         * 总论：
         *      1.segment的读操作不需要加锁，但需要volatile读
         *      2.当进行扩容时(调用reHash方法)，需要拷贝原始数据，在拷贝数据上操作，保证在扩容完成前读操作仍可以在原始数据上进行。
         *      3.只有引起数据变化的操作需要加锁。
         *      4.scanAndLock(删除、替换)/scanAndLockForPut(新增)两个方法提供了获取锁的途径，是通过自旋锁实现的。
         *      5.在等待获取锁的过程中，两个方法都会对目标数据进行查找，每次查找都会与上次查找的结果对比，虽然查找结果不会被调用它的方法使用，但是这样做可以减少后续操作可能的cache miss。
         *
         */
         
         
        // 自旋锁的等待次数上限，多处理器时64次，单处理器时1次。
        // 每次等待都会进行查询操作，当等待次数超过上限时，不再自旋，调用lock方法等待获取锁 
        static final int MAX_SCAN_RETRIES = Runtime.getRuntime().availableProcessors() > 1 ? 64 : 1;
        
        // Segment中的hash表，与hashMap结构相同，表中每个元素都是一个链表
        transient volatile HashEntry<K,V>[] table;
        
        // 表中元素个数
        transient int count;
        
        // 记录数据变化操作的次数.
        // 作用：这一数值主要为Map的isEmpty和size方法提供同步操作检查，这两个方法没有为全表加锁。
        //       在统计segment.count前后，都会统计segment.modCount，如果前后两次值发生变化，可以判断在统计count期间有segment发生了其它操作
        transient int modCount;
        
        // 容量阈值，超过这一数值后segment将进行扩容，容量变为原来的两倍
        // threshold = loadFactor * table.length
        transient int threshold;

        final float loadFactor;

        Segment(float lf, int threshold, HashEntry<K,V>[] tab) {
            this.loadFactor = lf;
            this.threshold = threshold;
            this.table = tab;
        }
        
        // onlyIfAbsent:若为true，当key已经有对应的value时，不进行替换；若为false，即使key已经有对应的value，仍进行替换。
        // 关于put方法，很重要的一点是segment最大长度的问题：
        // 代码 c > threshold && tab.length < MAXIMUM_CAPACITY 作为是否需要扩容的判断条件。
        // 扩容条件是node总数超过阈值且table长度小于MAXIMUM_CAPACITY也就是2的30次幂。
        // 由于扩容都是容量翻倍，所以tab.length最大值就是2的30次幂。此后，即使node总数超过了阈值，也不会扩容了。
        // 由于table[n]对应的是一个链表，链表内元素个数理论上是无限的，所以segment的node总数理论上也是无上限的。
        // ConcurrentHashMap的size()方法考虑到了这个问题，当计算结果超过Integer.MAX_VALUE时，直接返回Integer.MAX_VALUE.
        final V put(K key, int hash, V value, boolean onlyIfAbsent) {
            // tryLock判断是否已经获得锁.
            // 如果没有获得，调用scanAndLockForPut方法自旋等待获得锁。
            HashEntry<K,V> node = tryLock() ? null :
                scanAndLockForPut(key, hash, value);
            V oldValue;
            try {
                HashEntry<K,V>[] tab = table;
                // 计算key在表中的下标
                int index = (tab.length - 1) & hash;
                // 获取链表的第一个node
                HashEntry<K,V> first = entryAt(tab, index);
                for (HashEntry<K,V> e = first;;) {
                    // 链表下一个node不为空，比较key值是否相同
                    // 相同的，根据onlyIfAbsent决定是否替换已有的值
                    if (e != null) {
                        K k;
                        if ((k = e.key) == key ||
                            (e.hash == hash && key.equals(k))) {
                            oldValue = e.value;
                            if (!onlyIfAbsent) {
                                e.value = value;
                                ++modCount;
                            }
                            break;
                        }
                        e = e.next;
                    }
                    else {
                        // 链表遍历到最后一个node，仍没有找到key值相同的.
                        // 此时应当生成新的node，将node的next指向链表表头，这样新的node将处于链表的【表头】位置
                        if (node != null)
                            node.setNext(first);
                        else
                        // node为null，表明sca 表明scanAndLockForPut过程中找到了key值相同的nodenAndLockForPut过程中找到了key值相同的node
                        // 可以断定在等待获取锁的过程中，这个node被删除了，此时需要新建一个node
                            node = new HashEntry<K,V>(hash, key, value, first);
                        int c = count + 1;
                        // 此处进行是否扩容判断
                        // 没有超过阈值，直接加入链表表头。头插法。
                        if (c > threshold && tab.length < MAXIMUM_CAPACITY)
                            rehash(node);
                        else
                            setEntryAt(tab, index, node);
                        ++modCount;
                        count = c;
                        oldValue = null;
                        break;
                    }
                }
            } finally {
                unlock();
            }
            return oldValue;
        }

        @SuppressWarnings("unchecked")
        private void rehash(HashEntry<K,V> node) {
            // 拷贝table，所有操作都在oldTable上进行，不会影响无需获得锁的读操作
            HashEntry<K,V>[] oldTable = table;
            int oldCapacity = oldTable.length;
            // 容量翻倍
            int newCapacity = oldCapacity << 1;
            // 更新阈值
            threshold = (int)(newCapacity * loadFactor);
            HashEntry<K,V>[] newTable =
                (HashEntry<K,V>[]) new HashEntry[newCapacity];
            int sizeMask = newCapacity - 1;
            for (int i = 0; i < oldCapacity ; i++) {
                HashEntry<K,V> e = oldTable[i];
                if (e != null) {
                    HashEntry<K,V> next = e.next;
                    // 新的table下标，定位链表
                    int idx = e.hash & sizeMask;
                    if (next == null)   //  Single node on list
                        // 链表只有一个node，直接赋值
                        newTable[idx] = e;
                    else { // Reuse consecutive sequence at same slot
                        // 这里获取特殊node
                        HashEntry<K,V> lastRun = e;
                        int lastIdx = idx;
                        for (HashEntry<K,V> last = next;
                             last != null;
                             last = last.next) {
                            int k = last.hash & sizeMask;
                            if (k != lastIdx) {
                                lastIdx = k;
                                lastRun = last;
                            }
                        }
                        // 步骤一中的table[n]赋值过程
                        newTable[lastIdx] = lastRun;
                        // Clone remaining nodes
                        // 步骤二，遍历剩余node，插入对应表头
                        for (HashEntry<K,V> p = e; p != lastRun; p = p.next) {
                            V v = p.value;
                            int h = p.hash;
                            int k = h & sizeMask;
                            HashEntry<K,V> n = newTable[k];
                            newTable[k] = new HashEntry<K,V>(h, p.key, v, n);
                        }
                    }
                }
            }
            int nodeIndex = node.hash & sizeMask; // add the new node
            node.setNext(newTable[nodeIndex]);
            newTable[nodeIndex] = node;
            table = newTable;
        }

         // put方法调用本方法获取锁，通过自旋锁等待其他线程释放锁。
         // 变量retries记录自旋锁循环次数，当retries超过MAX_SCAN_RETRIES时，不再自旋，调用lock方法等待锁释放。
         // 变量first记录hash计算出的所在链表的表头node，每次循环结束，重新获取表头node，与first比较，如果发生变化，说明在自旋期间，有新的node插入了链表，retries计数重置。
         // 自旋过程中，会遍历链表，如果发现不存在对应key值的node，创建一个，这个新node可以作为返回值返回。
         //  根据官方注释，自旋过程中遍历链表是为了缓存预热，减少hash表经常出现的cache miss
        private HashEntry<K,V> scanAndLockForPut(K key, int hash, V value) {
            HashEntry<K,V> first = entryForHash(this, hash);
            HashEntry<K,V> e = first;
            HashEntry<K,V> node = null;
            int retries = -1; // negative while locating node
            while (!tryLock()) {
                HashEntry<K,V> f; // to recheck first below
                if (retries < 0) {
                    if (e == null) {
                        if (node == null) // speculatively create node 
                        // 链表为空或者遍历至链表最后一个node仍没有找到匹配
                            node = new HashEntry<K,V>(hash, key, value, null);
                        retries = 0;
                    }
                    else if (key.equals(e.key))
                        retries = 0;
                    else
                        e = e.next;
                }
                else if (++retries > MAX_SCAN_RETRIES) {
                    lock();
                    break;
                } // 比较first与新获得的链表表头node是否一致，如果不一致，说明该链表别修改过，自旋计数重置
                else if ((retries & 1) == 0 &&
                         (f = entryForHash(this, hash)) != first) {
                    e = first = f; // re-traverse if entry changed
                    retries = -1;
                }
            }
            return node;
        }

        // remove,replace方法会调用本方法获取锁，通过自旋锁等待其他线程释放锁。
        // 与scanAndLockForPut机制相似。
        private void scanAndLock(Object key, int hash) {
            // similar to but simpler than scanAndLockForPut
            HashEntry<K,V> first = entryForHash(this, hash);
            HashEntry<K,V> e = first;
            int retries = -1;
            while (!tryLock()) {
                HashEntry<K,V> f;
                if (retries < 0) {
                    if (e == null || key.equals(e.key))
                        retries = 0;
                    else
                        e = e.next;
                }
                else if (++retries > MAX_SCAN_RETRIES) {
                    lock();
                    break;
                }
                else if ((retries & 1) == 0 &&
                         (f = entryForHash(this, hash)) != first) {
                    e = first = f;
                    retries = -1;
                }
            }
        }

        // 删除key-value都匹配的node，删除过程很简单
        // 1.根据hash计算table下标index
        // 2.根据index定位链表，遍历链表node，如果存在node的key值和value值都匹配，删除该node。
        // 3.令node的前一个节点pred的pred.next = node.next。
        final V remove(Object key, int hash, Object value) {
            if (!tryLock())
                scanAndLock(key, hash);
            V oldValue = null;
            try {
                HashEntry<K,V>[] tab = table;
                int index = (tab.length - 1) & hash;
                HashEntry<K,V> e = entryAt(tab, index);
                HashEntry<K,V> pred = null;
                while (e != null) {
                    K k;
                    HashEntry<K,V> next = e.next;
                    if ((k = e.key) == key ||
                        (e.hash == hash && key.equals(k))) {
                        V v = e.value;
                        if (value == null || value == v || value.equals(v)) {
                            if (pred == null)
                                setEntryAt(tab, index, next);
                            else
                                pred.setNext(next);
                            ++modCount;
                            --count;
                            oldValue = v;
                        }
                        break;
                    }
                    pred = e;
                    e = next;
                }
            } finally {
                unlock();
            }
            return oldValue;
        }

        final boolean replace(K key, int hash, V oldValue, V newValue) {
            if (!tryLock())
                scanAndLock(key, hash);
            boolean replaced = false;
            try {
                HashEntry<K,V> e;
                for (e = entryForHash(this, hash); e != null; e = e.next) {
                    K k;
                    if ((k = e.key) == key ||
                        (e.hash == hash && key.equals(k))) {
                        if (oldValue.equals(e.value)) {
                            e.value = newValue;
                            ++modCount;
                            replaced = true;
                        }
                        break;
                    }
                }
            } finally {
                unlock();
            }
            return replaced;
        }

        final V replace(K key, int hash, V value) {
            if (!tryLock())
                scanAndLock(key, hash);
            V oldValue = null;
            try {
                HashEntry<K,V> e;
                for (e = entryForHash(this, hash); e != null; e = e.next) {
                    K k;
                    if ((k = e.key) == key ||
                        (e.hash == hash && key.equals(k))) {
                        oldValue = e.value;
                        e.value = value;
                        ++modCount;
                        break;
                    }
                }
            } finally {
                unlock();
            }
            return oldValue;
        }

        final void clear() {
            lock();
            try {
                HashEntry<K,V>[] tab = table;
                for (int i = 0; i < tab.length ; i++)
                    setEntryAt(tab, i, null);
                ++modCount;
                count = 0;
            } finally {
                unlock();
            }
        }
    }
```
#### 3.5 常见操作
1. put()
2. get()
3. remove()
### 四、总结

































