/**
 * Copyright © 2019, LeonKeh
 * <p>
 * All Rights Reserved.
 */

package com.concurrent.reentrantlock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 类功能描述
 *
 * @author Leon
 * @version 2019/2/11 11:52
 */
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
