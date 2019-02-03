## 基于CAS和Unsafe的AtomicInteger源码实现
当程序更新一个变量时，如果多线程同时更新这个变量，可能得到期望之外的值，比如变
量i=1，A线程更新i+1，B线程也更新i+1，经过两个线程操作之后可能i不等于3，而是等于2。因
为A和B线程在更新变量i的时候拿到的i都是1，这就是线程不安全的更新操作，通常我们会使
用synchronized来解决这个问题，synchronized会保证多线程不会同时更新变量i。

而Java从JDK 1.5开始提供了java.util.concurrent.atomic包（以下简称Atomic包），这个包中
的原子操作类提供了一种用法简单、性能高效、线程安全地更新一个变量的方式。

因为变量的类型有很多种，所以在Atomic包里一共提供了13个类，属于4种类型的原子更
新方式，分别是<font color='red'>原子更新基本类型、原子更新数组、原子更新引用和原子更新属性（字段）</font>。
Atomic包里的类基本都是使用Unsafe实现的包装类。
### 一、原子更新基本类型---以AtomicInteger为例
1. 作用

   提供<font color='red'>以原子的方式更新基本类型</font>。
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
- <font color='red'>Unsafe是CAS的核心，也是AtomicInteger的核心</font>
- <font color='red'>valueOffset是变量值在内存中的偏移地址，而Unsafe提供了相应的方法</font>
- <font color='red'>value是volatile修饰，其实这是真正存储值的变量</font>
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

5）在第四步中的过程中，即使在CAS操作时有线程3来抢占资源，但是也是无法抢占成功的,<font color='red'>因为compareAndSwapInt是一个原子操作</font>。

所以在整个的更新过程中通过了CAS保证了操作的原子性。

2. 其他方法都和1中类似

3. AtomicLong、AtomicBoolean等其他

<font color='red'>Atomic包提供了3种基本类型的原子更新，但是Java的基本类型里还有char、float和double
等。那么问题来了，如何原子的更新其他的基本类型呢？Atomic包里的类基本都是使用Unsafe
实现的</font>，让我们一起看一下Unsafe的源码。

```
    public final native boolean compareAndSwapObject(Object o, long offset,
                                                     Object expected,
                                                     Object x);

    public final native boolean compareAndSwapInt(Object o, long offset,
                                                  int expected,
                                                  int x);

    public final native boolean compareAndSwapLong(Object o, long offset,
                                                   long expected,
                                                   long x);
```

通过代码，我们发现Unsafe只提供了3种CAS方法：<font color='red'>compareAndSwapObject、compare-
AndSwapInt和compareAndSwapLong</font>，再看AtomicBoolean源码，发现它是先把Boolean转换成整
型，再使用compareAndSwapInt进行CAS，所以原子更新char、float和double变量也可以用类似
的思路来实现。

下面我们来看看AtomicBoolean的实现：

```
    public final boolean getAndSet(boolean newValue) {
        boolean prev;
        do {
            prev = get();
        } while (!compareAndSet(prev, newValue));
        return prev;
    }

    // 非0就是true,否则就是false
    public final boolean get() {
        return value != 0;
    }

    // 最后调用的是Unsafe类的compareAndSwapInt，把boolean转换成int类型进行运算
    public final boolean compareAndSet(boolean expect, boolean update) {
        int e = expect ? 1 : 0;
        int u = update ? 1 : 0;
        return unsafe.compareAndSwapInt(this, valueOffset, e, u);
    }
```
### 三、原子更新数组---以AtomicIntegerArray为例
1. 类型
   - AtomicIntegerArray  原子更新整形数组里的元素
   - AtomicLongArray   原子更新长整形数组里的元素
   - AtomicReferenceArray 原子更新引用类型数组里的元素
2. AtomicIntegerArray中常用方法
   1. addAndGet() 以原子方式将输入值与数组中索引i的元素相加
   ```
       public final int addAndGet(int i, int delta) {
           return getAndAdd(i, delta) + delta;
       }

       public final int getAndAdd(int i, int delta) {
           return unsafe.getAndAddInt(array, checkedByteOffset(i), delta);
       }

       // 最终还是利用Unsafe类中compareAndSwapInt方法实现的
       public final int getAndAddInt(Object o, long offset, int delta) {
           int v;
           do {
               v = getIntVolatile(o, offset);
           } while (!compareAndSwapInt(o, offset, v, v + delta));
           return v;
       }

       public final native boolean compareAndSwapInt(Object o, long offset,
                                                     int expected,
                                                     int x);
   ```
   2. compareAndSet(int i, int expect, int update)如果当前值等于预期值，则以原子方式将数组位置i的元素设置成update值
   ```
       public final boolean compareAndSet(int i, int expect, int update) {
           return compareAndSetRaw(checkedByteOffset(i), expect, update);
       }

       private boolean compareAndSetRaw(long offset, int expect, int update) {
           return unsafe.compareAndSwapInt(array, offset, expect, update);
       }
   ```
   3. 其他方法和之前的类似，这里省略
### 四、原子更新引用类型---以AtomicReference为例
1. 类型
   - AtomicReference   原子更新引用类
   - AtomicReferenceFieldUpdater   原子更新引用类型里的字段
   - AtomicMarkableReference：原子更新带有标记位的引用类型。<font color='red'>可以原子更新一个布尔类
     型的标记位和引用类型</font>。构造方法是AtomicMarkableReference（V initialRef，boolean
     initialMark）

   以上几个类提供的方法几乎一样，所以仅以AtomicReference为例进行讲解，
2. AtomicReference示例
   ```
   public class TestReference {
       private static AtomicReference<User> referenceUser = new AtomicReference<>();
       public static void main(String[] args) {
           User u1 = new User("tom", 11);
           User updateUser = new User("jack", 22);
           referenceUser.set(u1);
           referenceUser.compareAndSet(u1, updateUser);
           System.out.println(referenceUser.get().getName());
           System.out.println(referenceUser.get().getAge());
       }

       static class User {
           private String name;
           private int age;

           public User() {}

           public User(String name, int age) {
               this.name = name;
               this.age = age;
           }
           //省略get/set方法
       }
   }

   ```
### 五、原子更新字段类型
1. 类型
   如果需原子地更新某个类里的某个字段时，就需要使用原子更新字段类，Atomic包提供
   了以下3个类进行原子字段更新。

   - AtomicIntegerFieldUpdater：原子更新整型的字段的更新器
   - AtomicLongFieldUpdater：原子更新长整型字段的更新器
   - AtomicStampedReference：原子更新带有版本号的引用类型。该类将整数值与引用关联起
     来，可用于原子的更新数据和数据的版本号，可以解决使用CAS进行原子更新时可能出现的
     ABA问题。

   要想原子地更新字段类需要两步。<font color='red'>第一步，因为原子更新字段类都是抽象类，每次使用的
   时候必须使用静态方法newUpdater()创建一个更新器，并且需要设置想要更新的类和属性。第
   二步，更新类的字段（属性）必须使用public volatile修饰符</font



























