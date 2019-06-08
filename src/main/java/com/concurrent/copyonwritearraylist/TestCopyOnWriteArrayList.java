package com.concurrent.copyonwritearraylist;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 类功能描述
 *
 * @author Leon
 * @version 2019/6/8 21:50
 */
public class TestCopyOnWriteArrayList {

    public static void main(String[] args) throws Exception {
        List<Integer> list = new CopyOnWriteArrayList<>();
        WriteThread writeThread01 = new WriteThread(list);
        WriteThread writeThread02 = new WriteThread(list);
        WriteThread writeThread03 = new WriteThread(list);
        WriteThread writeThread04 = new WriteThread(list);
        WriteThread writeThread05 = new WriteThread(list);

        ReadThread readThread01 = new ReadThread(list);
        ReadThread readThread02 = new ReadThread(list);

        readThread01.start();
        writeThread01.start();
        writeThread02.start();
        writeThread03.start();
        writeThread04.start();
        readThread02.start();
        writeThread05.start();

        System.out.println("copyList size = " + list.size());
    }

    static class ReadThread extends Thread {

        private List<Integer> list;

        public ReadThread(List<Integer> list) {
            this.list = list;
        }

        @Override
        public void run() {
            for (Integer integer : list) {
                System.out.println("ReadThread = " + integer);
            }
        }
    }

    static class WriteThread extends Thread {

        private List<Integer> list;

        public WriteThread(List<Integer> list) {
            this.list = list;
        }

        @Override
        public void run() {
            int randomInt = new Random().nextInt();
            System.out.println("WriteThread add " + randomInt);
            list.add(randomInt);
        }
    }
}
