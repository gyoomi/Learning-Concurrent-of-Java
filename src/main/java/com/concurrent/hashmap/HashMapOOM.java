/**
 * Copyright © 2019, LeonKeh
 * <p>
 * All Rights Reserved.
 */

package com.concurrent.hashmap;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类功能描述
 *
 * @author Leon
 * @version 2019/2/25 11:52
 */
public class HashMapOOM {

    public static void main(String[] args) throws Exception {
        AtomicInteger ai = new AtomicInteger(0);
        HashMap<String, Object> map = new HashMap<>(1);
        HashMapThread t1 = new HashMapThread(ai, map);
        HashMapThread t2 = new HashMapThread(ai, map);
        HashMapThread t3 = new HashMapThread(ai, map);
        HashMapThread t4 = new HashMapThread(ai, map);
        HashMapThread t5 = new HashMapThread(ai, map);
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        System.out.println("main over");
    }
}

class HashMapThread extends Thread {

    private AtomicInteger ai;
    private HashMap<Integer, Object> map;

    public HashMapThread(AtomicInteger ai, HashMap map) {
        this.ai = ai;
        this.map = map;
    }

    @Override
    public void run() {
        while (ai.get() < 1000000) {
            map.put(ai.get(), "");
            ai.incrementAndGet();
        }
    }
}
