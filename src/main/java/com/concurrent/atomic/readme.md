## 基于CAS和Unsafe的AtomicInteger源码实现
当程序更新一个变量时，如果多线程同时更新这个变量，可能得到期望之外的值，比如变
量i=1，A线程更新i+1，B线程也更新i+1，经过两个线程操作之后可能i不等于3，而是等于2。因
为A和B线程在更新变量i的时候拿到的i都是1，这就是线程不安全的更新操作，通常我们会使
用synchronized来解决这个问题，synchronized会保证多线程不会同时更新变量i。

而Java从JDK 1.5开始提供了java.util.concurrent.atomic包（以下简称Atomic包），这个包中
的原子操作类提供了一种用法简单、性能高效、线程安全地更新一个变量的方式。

因为变量的类型有很多种，所以在Atomic包里一共提供了13个类，属于4种类型的原子更
新方式，分别是原子更新基本类型、原子更新数组、原子更新引用和原子更新属性（字段）。
Atomic包里的类基本都是使用Unsafe实现的包装类。
### 一、AtomicInteger基本概念
1. 作用

   提供以原子的方式更新基本类型。
2. 类型

   - AtomicBoolean  原子更新布尔类型
   - AtomicInteger  原子更新整型类型
   - AtomicLong  原子更新长整型类型
### 二、AtomicInteger初始化
```
    public class AtomicInteger extends Number implements java.io.Serializable {
        private static final long serialVersionUID = 6214790243416807050L;
    
        // setup to use Unsafe.compareAndSwapInt for updates
        // 这里可以看出，他是基于Unsafe类实现的
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        
        // 属性value值再内存中偏移量
        private static final long valueOffset;
    
        static {
            try {
                valueOffset = unsafe.objectFieldOffset
                    (AtomicInteger.class.getDeclaredField("value"));
            } catch (Exception ex) { throw new Error(ex); }
        }
    
        // AtomicInteger 本身是个整型，所以最重要的属性就是value
        private volatile int value;
    
        /**
         * Creates a new AtomicInteger with the given initial value.
         *
         * @param initialValue the initial value
         */
        public AtomicInteger(int initialValue) {
            value = initialValue;
        }
    
        /**
         * Creates a new AtomicInteger with initial value {@code 0}.
         */
        public AtomicInteger() {
        }
        
    }   
```
可以看出：
- Unsafe是CAS的核心，也是AtomicInteger的核心
- valueOffset是变量值在内存中的偏移地址，而Unsafe提供了相应的方法
- value是volatile修饰，其实这是真正存储值的变量
### 三、AtomicInteger常用方法
1. getAndIncrement() 获取后自增1
```
    public final int getAndIncrement() {
        return unsafe.getAndAddInt(this, valueOffset, 1);
    }
    
```
Unsafe类中：
```
    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param delta the value to add
     * @return the previous value
     * @since 1.8
     */
    public final int getAndAddInt(Object o, long offset, int delta) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, v + delta));
        return v;
    }
    
     /**
      * Atomically update Java variable to <tt>x</tt> if it is currently
      * holding <tt>expected</tt>.
      * @return <tt>true</tt> if successful
      */
     public final native boolean compareAndSwapInt(Object o, long offset,
                                                      int expected,
                                                      int x);
    
    
```
情景模拟：
假如现有一个new AtomicInteger(0);现在有线程1和线程2同时要对其执行getAndAddInt操作。
1）线程1先拿到值0，此时线程切换；

2）线程2拿到值也为0，此时调用Unsafe比较内存中的值也是0，比较成功，即进行+1的更新操作，即现在的值为1。线程切换；

3）线程1恢复运行，利用CAS发现自己的值是0，而内存中则是1。得到：此时值被另外一个线程修改，我不能进行修改；

4）线程1判断失败，继续循环取值，判断。因为volatile修饰value,所以再取到的值也是1。这是在执行CAS操作，发现expect和此时内存的值相等，修改成功，值为2

5）在第四步中的过程中，即使在CAS操作时有线程3来抢占资源，但是也是无法抢占成功的,因为compareAndSwapInt是一个原子操作。

所以在整个的更新过程中通过了CAS保证了操作的原子性。

2. 
### 三、其他


























