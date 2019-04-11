## 基于AQS的BlockingQueue及其他阻塞队列
### 一、什么是阻塞队列
1. 定义

   阻塞队列（BlockingQueue）是一个支持两个附加操作的队列。这两个附加的操作<font color='red'>支持阻塞
   的插入和移除方法</font>。

   1) 支持阻塞的插入方法：<font color='red'>意思是当队列满时，队列会阻塞插入元素的线程，直到队列不满</font>
   2) 支持阻塞的移除方法：<font color='red'>意思是在队列为空时，获取元素的线程会等待队列变为非空</font>
2. 适用场景

   阻塞队列常用于生产者和消费者的场景，生产者是向队列里添加元素的线程，消费者是
   从队列里取元素的线程。阻塞队列就是生产者用来存放元素、消费者用来获取元素的容器。
3. 4种插入和移除处理方法

   ![](./asserts/001.png)

   1) <font color='red'>抛出异常</font>：当队列满时，如果再往队列里插入元素，会抛出IllegalStateException（"Queue
   full"）异常。当队列空时，从队列里获取元素会抛出NoSuchElementException异常
   2) <font color='red'>返回特殊值</font>：当往队列插入元素时，会返回元素是否插入成功，成功返回true。如果是移
      除方法，则是从队列里取出一个元素，如果没有则返回null
   3) <font color='red'>一直阻塞</font>：当阻塞队列满时，如果生产者线程往队列里put元素，队列会一直阻塞生产者
      线程，直到队列可用或者响应中断退出。当队列空时，如果消费者线程从队列里take元素，队
      列会阻塞住消费者线程，直到队列不为空
   4) <font color='red'>超时退出</font>：当阻塞队列满时，如果生产者线程往队列里插入元素，队列会阻塞生产者线程
      一段时间，如果超过了指定的时间，生产者线程就会退出

   <font color='red'>注意</font>：如果是无界阻塞队列，队列不可能会出现满的情况，所以使用put或offer方法永
   远不会被阻塞，而且使用offer方法时，该方法永远返回true
### 二、Java里的阻塞队列
JDK中提供了7个阻塞队列（1.7版本），如下：
- ArrayBlockingQueue：一个由数组结构组成的有界阻塞队列
- LinkedBlockingQueue：一个由链表结构组成的有界阻塞队列
- PriorityBlockingQueue：一个支持优先级排序的无界阻塞队列
- DelayQueue：一个使用优先级队列实现的无界阻塞队列
- SynchronousQueue：一个不存储元素的阻塞队列
- LinkedTransferQueue：一个由链表结构组成的无界阻塞队列
- LinkedBlockingDeque：一个由链表结构组成的双向阻塞队列

1. ArrayBlockingQueue
   1. 定义

      ArrayBlockingQueue是一个用数组实现的有界阻塞队列。此队列按照先进先出（FIFO）的原
      则对元素进行排序。
   2. 说明

      <font color='red'>默认情况下不保证线程公平的访问队列，所谓公平访问队列是指阻塞的线程，可以按照
      阻塞的先后顺序访问队列，即先阻塞线程先访问队列。非公平性是对先等待的线程是非公平
      的，当队列可用时，阻塞的线程都可以争夺访问队列的资格，有可能先阻塞的线程最后才访问
      队列。为了保证公平性，通常会降低吞吐量。我们可以使用以下代码创建一个公平的阻塞队
      列。</font>
2. LinkedBlockingQueue
   1. 定义

      LinkedBlockingQueue是一个用链表实现的有界阻塞队列。此队列的默认和最大长度为
      Integer.MAX_VALUE。此队列按照先进先出的原则对元素进行排序。
3. PriorityBlockingQueue
   1. 定义

      PriorityBlockingQueue是一个支持优先级的无界阻塞队列。默认情况下元素采取自然顺序
      升序排列。也可以自定义类实现compareTo()方法来指定元素排序规则，或者初始化
      PriorityBlockingQueue时，指定构造参数Comparator来对元素进行排序。需要注意的是不能保证
      同优先级元素的顺序。
4. DelayQueue
   1. 定义

      DelayQueue是一个支持延时获取元素的无界阻塞队列。队列使用PriorityQueue来实现。队
      列中的元素必须实现Delayed接口，在创建元素时可以指定多久才能从队列中获取当前元素。
      只有在延迟期满时才能从队列中提取元素
   2. 适用场景

      1) 缓存系统的设计：可以用DelayQueue保存缓存元素的有效期，使用一个线程循环查询
      DelayQueue，一旦能从DelayQueue中获取元素时，表示缓存有效期到了。
      2) 定时任务调度：使用DelayQueue保存当天将会执行的任务和执行时间，一旦从
         DelayQueue中获取到任务就开始执行，比如TimerQueue就是使用DelayQueue实现的
   3. 使用

      1) 如何实现Delayed接口

         DelayQueue队列的元素必须实现Delayed接口。我们可以参考ScheduledThreadPoolExecutor
         里ScheduledFutureTask类的实现，一共有三步.

         第一步：在对象创建的时候，初始化基本数据。使用time记录当前对象延迟到什么时候可
         以使用，使用sequenceNumber来标识元素在队列中的先后顺序。代码如下。

         ```
            private final long sequenceNumber;

            ScheduledFutureTask(Runnable r, V result, long ns) {
                super(r, result);
                this.time = ns;
                this.period = 0;
                this.sequenceNumber = sequencer.getAndIncrement();
            }
            ScheduledFutureTask(Runnable r, V result, long ns, long period) {
                super(r, result);
                this.time = ns;
                this.period = period;
                this.sequenceNumber = sequencer.getAndIncrement();
            }

            ScheduledFutureTask(Callable<V> callable, long ns) {
                super(callable);
                this.time = ns;
                this.period = 0;
                this.sequenceNumber = sequencer.getAndIncrement();
            }
         ```

         第二步：实现getDelay方法，该方法返回当前元素还需要延时多长时间，单位是纳秒，代码
         如下。

         ```
            public long getDelay(TimeUnit unit) {
                return unit.convert(time - now(), NANOSECONDS);
            }
         ```

         通过构造函数可以看出延迟时间参数ns的单位是纳秒，自己设计的时候最好使用纳秒，因
         为实现getDelay()方法时可以指定任意单位，一旦以秒或分作为单位，而延时时间又精确不到
         纳秒就麻烦了。使用时请注意当time小于当前时间时，getDelay会返回负数。

         第三步：实现compareTo方法来指定元素的顺序。例如，让延时时间最长的放在队列的末
         尾。实现代码如下。

         ```
             public int compareTo(Delayed other) {
                 if (other == this) // compare zero if same object
                     return 0;
                 if (other instanceof ScheduledFutureTask) {
                     ScheduledFutureTask<?> x = (ScheduledFutureTask<?>)other;
                     long diff = time - x.time;
                     if (diff < 0)
                         return -1;
                     else if (diff > 0)
                         return 1;
                     else if (sequenceNumber < x.sequenceNumber)
                         return -1;
                     else
                         return 1;
                 }
                 long diff = getDelay(NANOSECONDS) - other.getDelay(NANOSECONDS);
                 return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
             }
         ```
      2) 如何实现延时阻塞队列

         延时阻塞队列的实现很简单，当消费者从队列里获取元素时，如果元素没有达到延时时
         间，就阻塞当前线程。

        DelayQueue类：
        ```
            public E take() throws InterruptedException {
                final ReentrantLock lock = this.lock;
                lock.lockInterruptibly();
                try {
                    for (;;) {
                        E first = q.peek();
                        if (first == null)
                            available.await();
                        else {
                            long delay = first.getDelay(NANOSECONDS);
                            if (delay <= 0)
                                return q.poll();
                            first = null; // don't retain ref while waiting
                            if (leader != null)
                                available.await();
                            else {
                                Thread thisThread = Thread.currentThread();
                                leader = thisThread;
                                try {
                                    available.awaitNanos(delay);
                                } finally {
                                    if (leader == thisThread)
                                        leader = null;
                                }
                            }
                        }
                    }
                } finally {
                    if (leader == null && q.peek() != null)
                        available.signal();
                    lock.unlock();
                }
            }
        ```

        代码中的变量leader是一个等待获取队列头部元素的线程。如果leader不等于空，表示已
        经有线程在等待获取队列的头元素。所以，使用await()方法让当前线程等待信号。如果leader
        等于空，则把当前线程设置成leader，并使用awaitNanos()方法让当前线程等待接收信号或等
        待delay时间。
5. SynchronousQueue
   1) 定义

      SynchronousQueue是一个不存储元素的阻塞队列。每一个put操作必须等待一个take操作，
      否则不能继续添加元素
   2) 说明

      它支持公平访问队列。默认情况下线程采用非公平性策略访问队列。使用以下构造方法
      可以创建公平性访问的SynchronousQueue，如果设置为true，则等待的线程会采用先进先出的
      顺序访问队列

      ```
          public SynchronousQueue() {
              this(false);
          }


          public SynchronousQueue(boolean fair) {
              transferer = fair ? new TransferQueue<E>() : new TransferStack<E>();
          }
      ```

      SynchronousQueue可以看成是一个传球手，负责把生产者线程处理的数据直接传递给消费
      者线程。队列本身并不存储任何元素，非常适合传递性场景。SynchronousQueue的吞吐量高于
      LinkedBlockingQueue和ArrayBlockingQueue
6. LinkedTransferQueue
   1) 定义

      LinkedTransferQueue是一个由链表结构组成的无界阻塞TransferQueue队列。相对于其他阻
      塞队列，LinkedTransferQueue多了tryTransfer和transfer方法
   2) transfer方法

      如果当前有消费者正在等待接收元素（消费者使用take()方法或带时间限制的poll()方法
      时），transfer方法可以把生产者传入的元素立刻transfer（传输）给消费者。如果没有消费者在等
      待接收元素，transfer方法会将元素存放在队列的tail节点，并等到该元素被消费者消费了才返
      回。transfer方法的关键代码如下

      ```
          Node pred = tryAppend(s, haveData);
             if (pred == null)
                 continue retry;           // lost race vs opposite mode
             if (how != ASYNC)
                 return awaitMatch(s, pred, e, (how == TIMED), nanos);
      ```

      第一行代码是试图把存放当前元素的s节点作为tail节点。第二行代码是让CPU自旋等待
      消费者消费元素。因为自旋会消耗CPU，所以自旋一定的次数后使用Thread.yield()方法来暂停
      当前正在执行的线程，并执行其他线程

   3) tryTransfer方法

      tryTransfer方法是用来试探生产者传入的元素是否能直接传给消费者。如果没有消费者等
      待接收元素，则返回false。和transfer方法的区别是tryTransfer方法无论消费者是否接收，方法
      立即返回，而transfer方法是必须等到消费者消费了才返回。

      对于带有时间限制的tryTransfer（E e，long timeout，TimeUnit unit）方法，试图把生产者传入
      的元素直接传给消费者，但是如果没有消费者消费该元素则等待指定的时间再返回，如果超
      时还没消费元素，则返回false，如果在超时时间内消费了元素，则返回true
7. LinkedBlockingDeque
   1) 定义

      LinkedBlockingDeque是一个由链表结构组成的双向阻塞队列。所谓双向队列指的是可以
      从队列的两端插入和移出元素。双向队列因为多了一个操作队列的入口，在多线程同时入队
      时，也就减少了一半的竞争。相比其他的阻塞队列，LinkedBlockingDeque多了addFirst、
      addLast、offerFirst、offerLast、peekFirst和peekLast等方法，以First单词结尾的方法，表示插入、
      获取（peek）或移除双端队列的第一个元素。以Last单词结尾的方法，表示插入、获取或移除双
      端队列的最后一个元素。另外，插入方法add等同于addLast，移除方法remove等效于
      removeFirst。但是take方法却等同于takeFirst，不知道是不是JDK的bug，使用时还是用带有First
      和Last后缀的方法更清楚
   2) 在初始化LinkedBlockingDeque时可以设置容量防止其过度膨胀。另外，双向阻塞队列可以
      运用在“工作窃取”模式中
### 三、阻塞队列的实现原理
如果队列是空的，消费者会一直等待，当生产者添加元素时，消费者是如何知道当前队列
有元素的呢？如果让你来设计阻塞队列你会如何设计，如何让生产者和消费者进行高效率的
通信呢？让我们先来看看JDK是如何实现的。
1. 使用通知模式实现

   所谓通知模式，就是当生产者往满的队列里添加元素时会阻塞住生
   产者，当消费者消费了一个队列中的元素后，会通知生产者当前队列可用。通过查看JDK源码
   发现ArrayBlockingQueue使用了Condition来实现，代码如下：

   ```
       final ReentrantLock lock;

       private final Condition notEmpty;

       private final Condition notFull;

       public ArrayBlockingQueue(int capacity) {
           this(capacity, false);
       }

       public ArrayBlockingQueue(int capacity, boolean fair) {
           if (capacity <= 0)
               throw new IllegalArgumentException();
           this.items = new Object[capacity];
           lock = new ReentrantLock(fair);
           notEmpty = lock.newCondition();
           notFull =  lock.newCondition();
       }

       public ArrayBlockingQueue(int capacity, boolean fair,
                                 Collection<? extends E> c) {
           this(capacity, fair);

           final ReentrantLock lock = this.lock;
           lock.lock(); // Lock only for visibility, not mutual exclusion
           try {
               int i = 0;
               try {
                   for (E e : c) {
                       checkNotNull(e);
                       items[i++] = e;
                   }
               } catch (ArrayIndexOutOfBoundsException ex) {
                   throw new IllegalArgumentException();
               }
               count = i;
               putIndex = (i == capacity) ? 0 : i;
           } finally {
               lock.unlock();
           }
       }

       public void put(E e) throws InterruptedException {
           checkNotNull(e);
           final ReentrantLock lock = this.lock;
           lock.lockInterruptibly();
           try {
               while (count == items.length)
                   notFull.await();
               enqueue(e);
           } finally {
               lock.unlock();
           }
       }

       public E take() throws InterruptedException {
           final ReentrantLock lock = this.lock;
           lock.lockInterruptibly();
           try {
               while (count == 0)
                   notEmpty.await();
               return dequeue();
           } finally {
               lock.unlock();
           }
       }


   ```

   当往队列里插入一个元素时，如果队列不可用，那么阻塞生产者主要通过
   LockSupport.park（this）来实现。

   ```
           // AQS中的数据
           public final void await() throws InterruptedException {
               if (Thread.interrupted())
                   throw new InterruptedException();
               Node node = addConditionWaiter();
               long savedState = fullyRelease(node);
               int interruptMode = 0;
               while (!isOnSyncQueue(node)) {
                   LockSupport.park(this);
                   if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                       break;
               }
               if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                   interruptMode = REINTERRUPT;
               if (node.nextWaiter != null) // clean up if cancelled
                   unlinkCancelledWaiters();
               if (interruptMode != 0)
                   reportInterruptAfterWait(interruptMode);
           }

        // LockSuppeort中的方法
       public static void park(Object blocker) {
           Thread t = Thread.currentThread();
           setBlocker(t, blocker);
           UNSAFE.park(false, 0L);
           setBlocker(t, null);
       }
   ```

   unsafe.park是个native方法，代码如下

   ```
   public native void park(boolean isAbsolute, long time);
   ```

   park这个方法会阻塞当前线程，只有以下4种情况中的一种发生时，该方法才会返回.

   - 与park对应的unpark执行或已经执行时。“已经执行”是指unpark先执行，然后再执行park
     的情况
   - 线程被中断时
   - 等待完time参数指定的毫秒数时
   - 异常现象发生时，这个异常现象没有任何原因

   再往下就是虚拟机的实现，具体实现的细节和方法我们在这里不过多讨论，感兴趣的可以查阅资料学习。
### 四、阻塞队列的实战---以ArrayBlockingQueue为例
1. 代码

```
/**
 * 实现生产者和消费者模式的自平衡
 *
 * @author Leon
 * @version 2019/2/13 15:40
 */
public class ArrayBlockingQueue01 {

    public static void main(String[] args) {
        BlockingQueue<Integer> bq = new ArrayBlockingQueue(10);
        Runnable produce01 = new Runnable(){
            int i = 0;
            @Override
            public void run() {
                for (;;) {
                    try {
                        System.out.println("生产者01生产了一个：" + i);
                        bq.put(i);
                        i++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Runnable customer01 = () -> {
            for (;;) {
                try {
                    System.out.println("消费者01消费了一个：" + bq.take());
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        Runnable customer02 = () -> {
            for (;;) {
                try {
                    System.out.println("消费者02消费了一个：" + bq.take());
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread t01 = new Thread(customer01);
        Thread t02 = new Thread(customer02);
        Thread t03 = new Thread(produce01);
        t01.start();
        t02.start();
        t03.start();
    }
}
```

2. 执行结果
```
生产者01生产了一个：0
消费者02消费了一个：0
生产者01生产了一个：1
消费者01消费了一个：1
生产者01生产了一个：2
消费者02消费了一个：2
生产者01生产了一个：3
消费者01消费了一个：3
生产者01生产了一个：4
消费者02消费了一个：4
生产者01生产了一个：5
消费者01消费了一个：5
生产者01生产了一个：6
消费者02消费了一个：6
生产者01生产了一个：7
消费者01消费了一个：7
生产者01生产了一个：8
消费者02消费了一个：8
生产者01生产了一个：9
...省略
```
3. 结论

通过平衡生产者和消费者的处理能力来提高整体处理数据的速度"，这给例子应该体现得很明显。
另外，也不要担心非单一生产者/消费者场景下的系统假死问题，
缓冲区空、缓冲区满的场景BlockingQueue都是定义了不同的Condition，所以不会唤醒自己的同类，就是第三部分
讲的原理的内容。






















