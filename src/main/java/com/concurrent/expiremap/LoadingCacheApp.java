package com.concurrent.expiremap;

import com.google.common.cache.*;

import java.util.concurrent.TimeUnit;

/**
 * 类功能描述
 *
 * @author Leon
 * @version 2020/2/26 22:27
 */
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
