package com.cigc.limit.service;

/**
 * Created by Administrator on 2018/7/4 0004.
 */
public class TestThread {
    private static String str="Thread-1";
    public static void main(String args[]) {
        for(int i=0;i<10;i++){
            String str1=str;
            //线程构造方法不接受静态变量
            ThreadDemo1 T1 = new ThreadDemo1( str1);
            T1.start();
        }



        /*T1 = new ThreadDemo1( "Thread-2");
        T1.start();*/
    }
}

class ThreadDemo1 extends Thread {

    private Thread t;
    private String threadName;


    public void start () {
        System.out.println("Starting " +  threadName );
        if (t == null) {
            t = new Thread (this, threadName);
            t.start ();
        }
    }



    ThreadDemo1( String name) {
        threadName = name;
        System.out.println("Creating " +  threadName );
    }

    public void run() {
        System.out.println("Running " +  threadName );
        try {
            for(int i = 4; i > 0; i--) {
                System.out.println("Thread: " + threadName + ", " + i);
                // 让线程睡眠一会
                Thread.sleep(50);
            }
        }catch (InterruptedException e) {
            System.out.println("Thread " +  threadName + " interrupted.");
        }
        System.out.println("Thread " +  threadName + " exiting.");
    }


}
