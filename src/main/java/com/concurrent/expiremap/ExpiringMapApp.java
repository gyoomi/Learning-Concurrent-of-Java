package com.concurrent.expiremap;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.util.concurrent.TimeUnit;

/**
 * 类功能描述
 *
 * @author Leon
 * @version 2020/2/26 22:11
 */
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
