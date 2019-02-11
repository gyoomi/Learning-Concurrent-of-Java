## ReentrantLock源码实现
### 一、示例使用
1. 示例
```
public class ReentrantLockDemo {

    public static void main(String[] args) throws Exception {
        Service service = new Service();
        Thread t01 = new Thread(() -> {
            service.doSomething();
        });
        Thread t02 = new Thread(() -> {
            service.doSomething();
        });
        t02.start();
        t01.start();
    }
}

class Service {

    private ReentrantLock lock = new ReentrantLock();

    public void doSomething() {
        try {
            lock.lock();
            System.out.println(Thread.currentThread().getName() + "  start do....");
            System.out.println(Thread.currentThread().getName() + "  end do....");
        } finally {
            lock.unlock();
        }
    }
}

```
2. 知识点
   1. 优点： 多路通知、嗅探
   2. 核心：lock()和unlock()为主线进行掌握
   3. 独占锁
   4. 继承AQS实现的自定义Sync
### 二、ReentrantLock的创建
1. 分类
   - 支持公平锁
   - 支持非公平锁
2. 创建
   - 公平锁
     ```
     ReentrantLock lock = new ReentrantLock(true);
     ```
   - 非公平锁
     ```
     ReentrantLock lock = new ReentrantLock();
     // 或
     ReentrantLock lock = new ReentrantLock(false);
     ```
3. 其他
   - 默认使用的是非公平锁
### 三、非公平锁的lock()
1. 代码
   ```
   lock.lock();
   ```
2. 总体思路
   - 基于CAS尝试将state（锁数量）从0设置为1
   - A、如果设置成功，设置当前线程为独占锁的线程；
   - B、如果设置失败，还会再获取一次锁数量，
   - B1、如果锁数量为0，再基于CAS尝试将state（锁数量）从0设置为1一次，如果设置成功，设置当前线程为独占锁的线程；
   - B2、如果锁数量不为0或者上边的尝试又失败了，查看当前线程是不是已经是独占锁的线程了，如果是，则将当前的锁数量+1；如果不是，则将该线程封装在一个Node内，并加入到等待队列中去。等待被其前一个线程节点唤醒。
3. 源代码
   1. ReentrantLock：lock()
      ```
          /**
           *获取一个锁
           *三种情况：
           *  1、如果当下这个锁没有被任何线程（包括当前线程）持有，则立即获取锁，锁数量==1，之后再执行相应的业务逻辑
           *  2、如果当前线程正在持有这个锁，那么锁数量+1，之后再执行相应的业务逻辑
           *  3、如果当下锁被另一个线程所持有，则当前线程处于休眠状态，直到获得锁之后，当前线程被唤醒，锁数量==1，再执行相应的业务逻辑
           */
          public void lock() {
              sync.lock();
          }
      ```
   2. NonfairSync：lock()
      ```
        /**
         * 1）首先基于CAS将state（锁数量）从0设置为1，如果设置成功，设置当前线程为独占锁的线程；-->请求成功-->第一次插队
         * 2）如果设置失败(即当前的锁数量可能已经为1了，即在尝试的过程中，已经被其他线程先一步占有了锁)，这个时候当前线程执行acquire(1)方法
         * 2.1）acquire(1)方法首先调用下边的tryAcquire(1)方法，在该方法中，首先获取锁数量状态，
         * 2.1.1）如果为0(证明该独占锁已被释放，当下没有线程在使用)，这个时候我们继续使用CAS将state（锁数量）从0设置为1，如果设置成功，当前线程独占锁；-->请求成功-->第二次插队；当然，如果设置不成功，直接返回false
         * 2.2.2）如果不为0，就去判断当前的线程是不是就是当下独占锁的线程，如果是，就将当前的锁数量状态值+1（这也就是可重入锁的名称的来源）-->请求成功
         *
         * 下边的流程一句话：请求失败后，将当前线程链入队尾并挂起，之后等待被唤醒。
         *
         * 2.2.3）如果最后在tryAcquire(1)方法中上述的执行都没成功，即请求没有成功，则返回false，继续执行acquireQueued(addWaiter(Node.EXCLUSIVE), arg)方法
         * 2.2）在上述方法中，首先会使用addWaiter(Node.EXCLUSIVE)将当前线程封装进Node节点node，然后将该节点加入等待队列（先快速入队，如果快速入队不成功，其使用正常入队方法无限循环一直到Node节点入队为止）
         * 2.2.1）快速入队：如果同步等待队列存在尾节点，将使用CAS尝试将尾节点设置为node，并将之前的尾节点插入到node之前
         * 2.2.2）正常入队：如果同步等待队列不存在尾节点或者上述CAS尝试不成功的话，就执行正常入队（该方法是一个无限循环的过程，即直到入队为止）-->第一次阻塞
         * 2.2.2.1）如果尾节点为空（初始化同步等待队列），创建一个dummy节点，并将该节点通过CAS尝试设置到头节点上去，设置成功的话，将尾节点也指向该dummy节点（即头节点和尾节点都指向该dummy节点）
         * 2.2.2.1）如果尾节点不为空，执行与快速入队相同的逻辑，即使用CAS尝试将尾节点设置为node，并将之前的尾节点插入到node之前
         * 最后，如果顺利入队的话，就返回入队的节点node，如果不顺利的话，无限循环去执行2.2)下边的流程，直到入队为止
         * 2.3）node节点入队之后，就去执行acquireQueued(final Node node, int arg)（这又是一个无限循环的过程，这里需要注意的是，无限循环等于阻塞，多个线程可以同时无限循环--每个线程都可以执行自己的循环，这样才能使在后边排队的节点不断前进）
         * 2.3.1）获取node的前驱节点p，如果p是头节点，就继续使用tryAcquire(1)方法去尝试请求成功，-->第三次插队（当然，这次插队不一定不会使其获得执行权，请看下边一条），
         * 2.3.1.1）如果第一次请求就成功，不用中断自己的线程，如果是之后的循环中将线程挂起之后又请求成功了，使用selfInterrupt()中断自己
         * （注意p==head&&tryAcquire(1)成功是唯一跳出循环的方法，在这之前会一直阻塞在这里，直到其他线程在执行的过程中，不断的将p的前边的节点减少，直到p成为了head且node请求成功了--即node被唤醒了，才退出循环）
         * 2.3.1.2）如果p不是头节点，或者tryAcquire(1)请求不成功，就去执行shouldParkAfterFailedAcquire(Node pred, Node node)来检测当前节点是不是可以安全的被挂起，
         * 2.3.1.2.1）如果node的前驱节点pred的等待状态是SIGNAL（即可以唤醒下一个节点的线程），则node节点的线程可以安全挂起，执行2.3.1.3）
         * 2.3.1.2.2）如果node的前驱节点pred的等待状态是CANCELLED，则pred的线程被取消了，我们会将pred之前的连续几个被取消的前驱节点从队列中剔除，返回false（即不能挂起），之后继续执行2.3）中上述的代码
         * 2.3.1.2.3）如果node的前驱节点pred的等待状态是除了上述两种的其他状态，则使用CAS尝试将前驱节点的等待状态设为SIGNAL，并返回false（因为CAS可能会失败，这里不管失败与否，都返回false，下一次执行该方法的之后，pred的等待状态就是SIGNAL了），之后继续执行2.3）中上述的代码
         * 2.3.1.3）如果可以安全挂起，就执行parkAndCheckInterrupt()挂起当前线程，之后，继续执行2.3）中之前的代码
         * 最后，直到该节点的前驱节点p之前的所有节点都执行完毕为止，我们的p成为了头节点，并且tryAcquire(1)请求成功，跳出循环，去执行。
         * （在p变为头节点之前的整个过程中，我们发现这个过程是不会被中断的）
         * 2.3.2）当然在2.3.1）中产生了异常，我们就会执行cancelAcquire(Node node)取消node的获取锁的意图。
         */
        final void lock() {
            if (compareAndSetState(0, 1)) // CAS尝试成功
                setExclusiveOwnerThread(Thread.currentThread()); // 将当前线程设置成独占线程
            else
                acquire(1);
        }
      ```
   3. AbstractQueuedSynchronizer：锁数量state属性+相关方法
      ```
          // volatile修饰，保证了可见性
          private volatile int state;

          protected final int getState() {
              return state;
          }

          protected final void setState(int newState) {
              state = newState;
          }

          protected final boolean compareAndSetState(int expect, int update) {
              // See below for intrinsics setup to support this
              return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
          }
      ```
   4. AbstractOwnableSynchronizer：属性+setExclusiveOwnerThread(Thread t)
      ```
       private transient Thread exclusiveOwnerThread;

       protected final void setExclusiveOwnerThread(Thread thread) {
           exclusiveOwnerThread = thread;
       }

       protected final Thread getExclusiveOwnerThread() {
           return exclusiveOwnerThread;
       }
      ```
   5. AbstractQueuedSynchronizer：acquire(int arg)
      ```
        public final void acquire(int arg) {
            if (!tryAcquire(arg) &&
                acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
                selfInterrupt(); // 自我中断
        }
      ```
   6. NonfairSync：tryAcquire(int acquires)
      ```
      protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
      }

      // Sync内部类：
      // 非公平锁中被tryAcquire调用
      final boolean nonfairTryAcquire(int acquires) {
          final Thread current = Thread.currentThread(); // 获取当前线程
          int c = getState(); // 获取锁数量
          if (c == 0) { // 如果锁数量为0，证明该独占锁已被释放，当下没有线程在使用
              if (compareAndSetState(0, acquires)) {
                  setExclusiveOwnerThread(current);
                  return true;
              }
          }
          else if (current == getExclusiveOwnerThread()) { // 查看当前线程是不是就是独占锁的线程;在这里可以看出了其是一种可重入性锁
              int nextc = c + acquires; // 如果是，锁状态的数量为当前的锁数量+1
              if (nextc < 0) // overflow
                  throw new Error("Maximum lock count exceeded");
              setState(nextc);
              return true;
          }
          return false; // 不能成功获取锁
      }
      ```

      <font color='red'>注意：这个方法就完成了"简化版的步骤"中的"A/B/B1"三步，如果上述的请求不能成功，就要执行下边的代码了</font>

      下边的代码，用一句话介绍：<font color='red'>请求失败后，将当前线程链入队尾并挂起，之后等待被唤醒。</font>在你看下边的代码的时候心里默记着这句话
   7. AbstractQueuedSynchronizer：addWaiter(Node mode)
      ```
          /**
           * 将Node节点加入等待队列
           *   1）快速入队，入队成功的话，返回node
           *   2）入队失败的话，使用正常入队
           *
           * 注意：
           *      快速入队与正常入队相比，可以发现，正常入队仅仅比快速入队多而一个判断队列是否为空且为空之后的过程，其他的逻辑是相同的
           * @return 返回当前要插入的这个节点，注意不是前一个节点
           */
          private Node addWaiter(Node mode) {
              //创建节点
              // 这里是Exclusive模式
              Node node = new Node(Thread.currentThread(), mode);
              // 将尾节点赋给pred
              Node pred = tail;
              if (pred != null) { // 尾节点不为空
                  node.prev = pred; // 将尾节点作为创造出来的节点的前一个节点，即将node链接到为节点后
                  // 基于CAS将node设置为尾节点，如果设置失败，说明在当前线程获取尾节点到现在这段过程中已经有其他线程将尾节点给替换过了
                  // 注意：假设有链表node1-->node2-->pred（当然是双链表，这里画成双链表才合适）
                  // 通过CAS将pred替换成了node节点，即当下的链表为node1-->node2-->node
                  // 然后根据上边的"node.prev = pred"与下边的"pred.next = node"将pred插入到双链表中去，组成最终的链表如下
                  // node1-->node2-->pred-->node
                  // 这样的话，实际上我们发现没有指定node2.next=pred与pred.prev=node2，这是为什么呢？
                  // 因为在之前这两句就早就执行好了，即node2.next和pred.prev这连个属性之前就设置好了
                  if (compareAndSetTail(pred, node)) {
                      pred.next = node;
                      return node;
                  }
              }
              enq(node); // 快速入队失败；正常入队
              return node;
          }
      ```
   8. AbstractQueuedSynchronizer：enq(final Node node)
      ```
          // 正常入队
          // 返回之前的尾节点
          private Node enq(final Node node) {
              // "自旋"直到入队成功为止
              for (;;) {
                  Node t = tail; // 获取尾节点
                  if (t == null) { //如果尾节点为null，说明当前等待队列为空
                      // 基于CAS将新节点（一个dummy节点）设置到头上head去，如果发现内存中的当前值不是null，则说明，在这个过程中，已经有其他线程设置过了
                      // 当成功的将这个dummy节点设置到head节点上去时，我们又将这个head节点设置给了tail节点，即head与tail都是当前这个dummy节点，
                      // 之后有新节点入队的话，就插入到该dummy之后
                      if (compareAndSetHead(new Node()))
                          tail = head;
                  } else { // 这一块儿的逻辑与快速入队完全相同
                      node.prev = t;
                      if (compareAndSetTail(t, node)) {
                          t.next = node;
                          return t;
                      }
                  }
              }
          }
      ```
   9. AbstractQueuedSynchronizer：acquireQueued(final Node node, int arg)
      ```
          // 无限循环（一直阻塞），直到node的前驱节点p之前的所有节点都执行完毕，p成为了head且node请求成功了
          final boolean acquireQueued(final Node node, int arg) {
              boolean failed = true;
              try {
                  boolean interrupted = false;
                  for (;;) {
                      // 获取插入节点的前一个节点p
                      final Node p = node.predecessor();
                      // 注意：
                      //     1、这个是跳出循环的唯一条件，除非抛异常
                      //     2、如果p == head && tryAcquire(arg)第一次循环就成功了，interrupted为false，不需要中断自己
                      //        如果p == head && tryAcquire(arg)第一次以后的循环中如果执行了挂起操作后才成功了，interrupted为true，就要中断自己了
                      if (p == head && tryAcquire(arg)) {
                          setHead(node);  // 当前节点设置为头节点
                          p.next = null; // help GC
                          failed = false;
                          return interrupted; // 跳出循环
                      }
                      if (shouldParkAfterFailedAcquire(p, node) &&
                          parkAndCheckInterrupt())
                          interrupted = true; // 被中断了
                  }
              } finally {
                  if (failed)
                      cancelAcquire(node);
              }
          }
      ```
   10. AbstractQueuedSynchronizer：shouldParkAfterFailedAcquire(Node pred, Node node)
       ```
       // 检测当前节点是否可以被安全的挂起（阻塞）
       // pred    当前节点的前驱节点
       // node    当前节点
       private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
               // 获取前驱节点（即当前线程的前一个节点）的等待状态
               int ws = pred.waitStatus;
               // 如果前驱节点的等待状态是SIGNAL，表示当前节点将来可以被唤醒，那么当前节点就可以安全的挂起了
               if (ws == Node.SIGNAL)
                   return true;

               // 1)当ws>0(即CANCELLED==1），前驱节点的线程被取消了，我们会将该节点之前的连续几个被取消的前驱节点从队列中剔除，返回false（即不能挂起）
               // 2)如果ws<=0&&!=SIGNAL,将当前节点的前驱节点的等待状态设为SIGNAL
               if (ws > 0) {
                   do {
                       node.prev = pred = pred.prev;
                   } while (pred.waitStatus > 0);
                   pred.next = node;
               } else {
                   // 尝试将当前节点的前驱节点的等待状态设为SIGNAL
                   // 1)这为什么用CAS，现在已经入队成功了，前驱节点就是pred，除了node外应该没有别的线程在操作这个节点了，那为什么还要用CAS？而不直接赋值呢？
                   //   解释：因为pred可以自己将自己的状态改为cancel，也就是pred的状态可能同时会有两条线程（pred和node）去操作
                   // 2)既然前驱节点已经设为SIGNAL了，为什么最后还要返回false
                   // 因为CAS可能会失败，这里不管失败与否，都返回false，下一次执行该方法的之后，pred的等待状态就是SIGNAL了
                   compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
               }
               return false;
           }
       ```
   11. AbstractQueuedSynchronizer：
       ```
           private final boolean parkAndCheckInterrupt() {
               LockSupport.park(this); // 挂起当前的线程
               return Thread.interrupted(); // 如果当前线程已经被中断了，返回true
           }
       ```

   以上就是一个线程获取非公平锁的整个过程（lock()）。
### 四、公平锁的lock()
1. 概述

   具体用法与非公平锁一样

   如果掌握了非公平锁的流程，那么掌握公平锁的流程会非常简单，只有两点不同（最后会讲）。
2. 总体思路
   - 获取一次锁数量，
   - B1、如果锁数量为0，如果当前线程是等待队列中的头节点，基于CAS尝试将state（锁数量）从0设置为1一次，如果设置成功，设置当前线程为独占锁的线程；
   - B2、如果锁数量不为0或者当前线程不是等待队列中的头节点或者上边的尝试又失败了，查看当前线程是不是已经是独占锁的线程了，如果是，则将当前的锁数量+1；如果不是，则将该线程封装在一个Node内，并加入到等待队列中去。等待被其前一个线程节点唤醒
3. 源代码
   1. ReentrantLock：lock()
      ```
         /**
           *获取一个锁
           *三种情况：
           *  1、如果当下这个锁没有被任何线程（包括当前线程）持有，则立即获取锁，锁数量==1，之后被唤醒再执行相应的业务逻辑
           *  2、如果当前线程正在持有这个锁，那么锁数量+1，之后被唤醒再执行相应的业务逻辑
           *  3、如果当下锁被另一个线程所持有，则当前线程处于休眠状态，直到获得锁之后，当前线程被唤醒，锁数量==1，再执行相应的业务逻辑
           */
          public void lock() {
              sync.lock();//调用FairSync的lock()方法
          }
      ```
   2. FairSync：lock()
      ```
      final void lock() {
            acquire(1);
      }
      ```
   3. AbstractQueuedSynchronizer：acquire(int arg)就是非公平锁使用的那个方法
   4. FairSync：tryAcquire(int acquires)
      ```
              /**
               * 获取公平锁的方法
               *
               * 1）获取锁数量c
               *    1.1)如果c==0，如果当前线程是等待队列中的头节点，使用CAS将state（锁数量）从0设置为1，如果设置成功，当前线程独占锁-->请求成功
               *    1.2)如果c!=0，判断当前的线程是不是就是当下独占锁的线程，如果是，就将当前的锁数量状态值+1（这也就是可重入锁的名称的来源）-->请求成功
               *    最后，请求失败后，将当前线程链入队尾并挂起，之后等待被唤醒。
               */
              protected final boolean tryAcquire(int acquires) {
                  final Thread current = Thread.currentThread();
                  int c = getState();
                  if (c == 0) {
                      if (!hasQueuedPredecessors() &&
                          compareAndSetState(0, acquires)) {
                          setExclusiveOwnerThread(current);
                          return true;
                      }
                  }
                  else if (current == getExclusiveOwnerThread()) {
                      int nextc = c + acquires;
                      if (nextc < 0)
                          throw new Error("Maximum lock count exceeded");
                      setState(nextc);
                      return true;
                  }
                  return false;
              }
      ```

      下边的代码与非公平锁一样。
### 五、公平锁与非公平锁对比
1. FairSync：lock()少了插队部分（即少了CAS尝试将state从0设为1，进而获得锁的过程）
2. FairSync：tryAcquire(int acquires)多了需要判断当前线程是否在等待队列首部的逻辑（实际上就是少了再次插队的过程，但是CAS获取还是有的）
3. ReentrantLock是基于AbstractQueuedSynchronizer实现的，所以不了解AQS的请看这里 [AbstractQueuedSynchronizer(AQS)源码实现](https://blog.csdn.net/weixin_39723544/article/details/86774397)
### 六、释放锁unlock()
1. 代码
   ```
   lock.unlock();
   ```
2. 总体思路
   1. 获取当前的锁数量，然后用这个锁数量减去解锁的数量（这里为1），最后得出结果c
   2. 判断当前线程是不是独占锁的线程，如果不是，抛出异常
   3. 如果c==0，说明锁被成功释放，将当前的独占线程置为null，锁数量置为0，返回true
   4. 如果c!=0，说明释放锁失败，锁数量置为c，返回false
   5. 如果锁被释放成功的话，唤醒距离头节点最近的一个非取消的节点(Node)
3. 源代码
   1. ReentrantLock：unlock()
      ```
          // 释放这个锁
          //   1）如果当前线程持有这个锁，则锁数量被递减
          //   2）如果递减之后锁数量为0，则锁被释放
          // 如果当前线程不持久有这个锁，抛出异常
          public void unlock() {
              sync.release(1);
          }
      ```
   2. AbstractQueuedSynchronizer：release(int arg)
      ```
          // 释放锁（在独占模式下）
          public final boolean release(int arg) {
              if (tryRelease(arg)) { // 如果成功释放锁
                  Node h = head; // 获取头节点：（注意：这里的头节点就是当前正在释放锁的节点）
                  if (h != null && h.waitStatus != 0) // 头结点存在且等待状态不是取消
                      unparkSuccessor(h); // 醒距离头节点最近的一个非取消的节点
                  return true;
              }
              return false;
          }
      ```
   3. ReentrantLock中Sync：tryRelease(int releases)
      ```
              // 释放锁
              protected final boolean tryRelease(int releases) {
                  int c = getState() - releases;  // 获取现在的锁数量-传入的解锁数量（这里为1）
                  if (Thread.currentThread() != getExclusiveOwnerThread())  // 当前线程不持有锁
                      throw new IllegalMonitorStateException();
                  boolean free = false;
                  if (c == 0) { // 锁被释放
                      free = true;
                      setExclusiveOwnerThread(null);
                  }
                  setState(c);
                  return free;
              }
      ```
   4. AbstractQueuedSynchronizer：unparkSuccessor(Node node)
      ```
          // 唤醒离头节点node最近的一个非取消的节点(从尾部往前找)
          private void unparkSuccessor(Node node) {
              int ws = node.waitStatus;
              if (ws < 0) // 将ws设为0状态（即什么状态都不是）
                  compareAndSetWaitStatus(node, ws, 0);
              // 获取头节点的下一个等待状态不是cancel的节点
              Node s = node.next;
              if (s == null || s.waitStatus > 0) {
                  s = null;
                  // 注意：从后往前遍历找到离头节点最近的一个非取消的节点，从后往前遍历据说是在入队（enq()）的时候，可能node.next==null
                  for (Node t = tail; t != null && t != node; t = t.prev)
                      if (t.waitStatus <= 0)
                          s = t;
              }
              if (s != null) // 唤醒离头节点最近的一个非取消的节点
                  LockSupport.unpark(s.thread);
          }
      ```
   5. 其他
      1. lock()多少次必须unlock()多少次。（体现了可重入性）尤其在递归调用中。
      2. 为什么从尾部往前找到离head最近的非取消的节点
         举个例子：
         队列如下：
                 Node1 <-> Node2 <-> Node3
         假如说我们现在从Node1走到Node3这个节点时，这是有新的节点入队。
         ```
             private Node addWaiter(Node mode) {
                 Node node = new Node(Thread.currentThread(), mode);
                 // Try the fast path of enq; backup to full enq on failure
                 Node pred = tail;
                 if (pred != null) {
                     node.prev = pred;
                     if (compareAndSetTail(pred, node)) { // 步骤1
                         pred.next = node;  // 步骤2
                         return node;
                     }
                 }
                 enq(node);
                 return node;
             }
         ```
         假如说走到了步骤1时，而此时主程序在从前往后找第一个非取消节点的时候，这时clh队列Node1 <-> Node2 <-> Node4。（因为步骤2此时没有执行，Node3节点就没了）
         但是此时你要是从后往前找的话，就避免了这种情况的存在。
         ```
         for (Node t = tail; t != null && t != node; t = t.prev)
             if (t.waitStatus <= 0)
                 s = t;
         ```
























