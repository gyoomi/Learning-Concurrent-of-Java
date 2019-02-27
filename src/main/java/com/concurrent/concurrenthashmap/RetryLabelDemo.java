/**
 * Copyright © 2019, LeonKeh
 * <p>
 * All Rights Reserved.
 */

package com.concurrent.concurrenthashmap;

/**
 * 类功能描述
 *
 * @author Leon
 * @version 2019/2/27 13:43
 */
public class RetryLabelDemo {
    public static void main(String[] args) throws Exception {
        test02();
    }

    //        0
    //        1
    //        2
    public static void test01() {
        exit:
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                System.out.println(j);
                if (j == 2) break exit;
            }
            System.out.println("--------");
        }
    }

    //        0
    //        1
    //        2
    //        0
    //        1
    //        2
    //        0
    //        1
    //        2
    public static void test02() {
        exit:
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                System.out.println(j);
                if (j == 2) continue exit;
            }
            System.out.println("--------");
        }
    }

    //            0
    //            1
    //            2
    //            3
    //            4
    //            -------------
    //            0
    //            1
    //            2
    //            3
    //            4
    //            -------------
    //            0
    //            1
    //            2
    //            3
    //            4
    //            -------------
    public static void test03() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                System.out.println(j);
                if (j == 2) continue;
            }
            System.out.println("-------------");
        }
    }

    //            0
    //            1
    //            2
    //            -------------
    //            0
    //            1
    //            2
    //            -------------
    //            0
    //            1
    //            2
    //            -------------
    public static void test04() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                System.out.println(j);
                if (j == 2) break;
            }
            System.out.println("-------------");
        }
    }
}
