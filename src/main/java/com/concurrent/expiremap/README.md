# Java基础之可设置过期时间的map

## 一、技术背景

在实际的项目开发中，我们经常会使用到缓存中间件（如redis、MemCache等）来帮助我们提高系统的可用性和健壮性。

但是很多时候如果项目比较简单，就没有必要为了使用缓存而专门引入Redis等等中间件来加重系统的复杂性。那么Java本身有没有好用的轻量级的缓存组件呢。

答案当然是有喽，而且方法不止一种。常见的解决方法有：**ExpiringMap、LoadingCache及基于HashMap的封装**三种。

## 二、技术效果

- 实现缓存的常见功能，如过时删除策略
- 热点数据预热

## 三、ExpiringMap

### 3.1 功能简介

1. 可设置Map中的Entry在一段时间后自动过期。
2. 可设置Map最大容纳值，当到达Maximum size后，再次插入值会导致Map中的第一个值过期。
3. 可添加监听事件，在监听到Entry过期时调度监听函数。
4. 可以设置懒加载，在调用get()方法时创建对象。

### 3.2 源码

[github地址](https://github.com/jhalterman/expiringmap/)

### 3.3 示例

1. 添加依赖（Maven）

```xml
<dependency> 
    <groupId>net.jodah</groupId> 
    <artifactId>expiringmap</artifactId> 
    <version>0.5.8</version> 
</dependency> 
```

2. 示例源码

```java
public class ExpiringMapApp {

	public static void main(String[] args) {
		// maxSize: 设置最大值,添加第11个entry时，会导致第1个立马过期(即使没到过期时间)
		// expiration：设置每个key有效时间10s, 如果key不设置过期时间，key永久有效。
		// variableExpiration: 允许更新过期时间值,如果不设置variableExpiration，不允许后面更改过期时间,一旦执行更改过期时间操作会抛异常UnsupportedOperationException
		// policy:
		//        CREATED: 只在put和replace方法清零过期时间
		//        ACCESSED: 在CREATED策略基础上增加, 在还没过期时get方法清零过期时间。
		//        清零过期时间也就是重置过期时间，重新计算过期时间.
		ExpiringMap<String, String> map = ExpiringMap.builder()
			.maxSize(10)
			.expiration(10, TimeUnit.SECONDS)
			.variableExpiration().expirationPolicy(ExpirationPolicy.CREATED).build();

		map.put("token", "lkj2412lj1412412nmlkjl2n34l23n4");
		map.put("name", "管理员", 20000, TimeUnit.SECONDS);

		// 模拟线程等待...
		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("token ===> " + map.get("token"));
		System.out.println("name ===> " + map.get("name"));

		// 注意： 在创建map时，指定的那些参数如过期时间和过期策略都是全局的, 对map中添加的每一个entry都适用.
		//        在put一个entry键值对时可以对当前entry 单独设置 过期时间、过期策略,只对当前这个entry有效.
	}
}

```

运行结果

```text
token ===> null
name ===> 管理员
```

3. 注意

- 在创建map时，指定的那些参数如过期时间和过期策略都是全局的, 对map中添加的每一个entry都适用。
- 在put一个entry键值对时可以对当前entry 单独设置 过期时间、过期策略,只对当前这个entry有效.

## 四、LoadingCache

### 4.1 功能简介

Google开源出来的一个线程安全的本地缓存解决方案。

特点：提供缓存回收机制，监控缓存加载/命中情况，灵活强大的功能，简单易上手的api。

### 4.2 示例

1. 源码

```java
public class LoadingCacheApp {

	public static void main(String[] args) throws Exception {
		// maximumSize: 缓存池大小，在缓存项接近该大小时， Guava开始回收旧的缓存项
		// expireAfterAccess: 设置时间对象没有被读/写访问则对象从内存中删除(在另外的线程里面不定期维护)
		// removalListener: 移除监听器,缓存项被移除时会触发的钩子
		// recordStats: 开启Guava Cache的统计功能
		LoadingCache<String, String> cache = CacheBuilder.newBuilder()
			.maximumSize(100)
			.expireAfterAccess(10, TimeUnit.SECONDS)
			.removalListener(new RemovalListener<String, String>() {
				@Override
				public void onRemoval(RemovalNotification<String, String> removalNotification) {
					System.out.println("过时删除的钩子触发了... key ===> " + removalNotification.getKey());
				}
			})
			.recordStats()
			.build(new CacheLoader<String, String>() {
				// 处理缓存键不存在缓存值时的处理逻辑
				@Override
				public String load(String key) throws Exception {
					return "不存在的key";
				}
			});

		cache.put("name", "小明");
		cache.put("pwd", "112345");

		// 模拟线程等待...
		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("token ===> " + cache.get("name"));
		System.out.println("name ===> " + cache.get("pwd"));
	}
}
```

2. 运行结果

```text
过时删除的钩子触发了... key ===> name
token ===> 不存在的key
过时删除的钩子触发了... key ===> pwd
name ===> 不存在的key
```

### 4.3 移除机制

guava做cache时候数据的移除分为**被动移除**和**主动移除**两种。

**被动移除**

- 基于大小的移除：数量达到指定大小，会把不常用的键值移除
- 基于时间的移除：expireAfterAccess(long, TimeUnit) 根据某个键值对最后一次访问之后多少时间后移除。expireAfterWrite(long, TimeUnit) 根据某个键值对被创建或值被替换后多少时间移除
- 基于引用的移除：主要是基于java的垃圾回收机制，根据键或者值的引用关系决定移除

**主动移除**

- 单独移除：Cache.invalidate(key)
- 批量移除：Cache.invalidateAll(keys)
- 移除所有：Cache.invalidateAll()

如果配置了移除监听器`RemovalListener`，则在所有移除的动作时会同步执行该`listener`下的逻辑。

如需改成异步，使用：`RemovalListeners.asynchronous(RemovalListener, Executor)`.

### 4.4 其他

1. 在put操作之前，如果已经有该键值，会先触发removalListener移除监听器，再添加
2. 配置了expireAfterAccess和expireAfterWrite，但在指定时间后没有被移除。

解决方案：CacheBuilder在文档上有说明：If expireAfterWrite or expireAfterAccess is requested entries may be evicted on each cache modification, on occasional cache accesses, or on calls to Cache.cleanUp(). Expired entries may be counted in Cache.size(), but will never be visible to read or write operations. 翻译过来大概的意思是：CacheBuilder构建的缓存不会在特定时间自动执行清理和回收工作，也不会在某个缓存项过期后马上清理，它不会启动一个线程来进行缓存维护，因为a）线程相对较重，b）某些环境限制线程的创建。它会在写操作时顺带做少量的维护工作，或者偶尔在读操作时做。当然，也可以创建自己的维护线程，以固定的时间间隔调用Cache.cleanUp()。

## 五、HashMap的封装

我们可以参考上面两个工具包的思路，自己封装一个可以设置过时时间的HashMap来实现我们想要的效果。
