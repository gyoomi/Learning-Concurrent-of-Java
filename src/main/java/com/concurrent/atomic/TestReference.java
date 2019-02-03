/**
 * Copyright © 2019, LeonKeh
 * <p>
 * All Rights Reserved.
 */

package com.concurrent.atomic;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 类功能描述
 *
 * @author Leon
 * @version 2019/2/2 11:19
 */
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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
