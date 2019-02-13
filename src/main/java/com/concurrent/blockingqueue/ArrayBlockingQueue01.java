/**
 * Copyright © 2019, LeonKeh
 * <p>
 * All Rights Reserved.
 */

package com.concurrent.blockingqueue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
