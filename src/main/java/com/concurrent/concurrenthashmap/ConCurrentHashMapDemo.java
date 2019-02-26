/**
 * Copyright © 2019, LeonKeh
 * <p>
 * All Rights Reserved.
 */

package com.concurrent.concurrenthashmap;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 类功能描述
 *
 * @author Leon
 * @version 2019/2/26 10:48
 */
public class ConCurrentHashMapDemo {

    public static void main(String[] args) throws Exception {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        map.put("hello", "v1");
        map.put("world", "v2");
        map.put("test", "v3");
        System.out.println(map);
    }
}
