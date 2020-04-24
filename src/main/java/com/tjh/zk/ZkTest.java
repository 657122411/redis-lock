package com.tjh.zk;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;

import java.security.spec.ECField;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ZkTest implements Runnable, Lock {
    static int inventory = 10;
    private static final int NUM = 10;
    private static CountDownLatch cdl = new CountDownLatch(1);

    private static final String IP_PORT = "127.0.0.1:2181";
    private static final String Z_NODE = "/LOCK";

    private static ZkClient zkClient = new ZkClient(IP_PORT);


    public static void main(String[] args) {
        try {

            for (int i = 1; i <= NUM; i++) {
                new Thread(new ZkTest()).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            new ZkTest().lock();
            if (inventory > 0) {
                inventory--;
            }
            System.out.println(inventory);
            return;
        } finally {
            new ZkTest().unlock();
            System.out.println("释放锁");
        }
    }

    public void lock() {
        // 尝试加锁
        if(tryLock()){
            return;
        }
        // 进入等待 监听
        waitForLock();
        // 再次尝试
        lock();
    }

    public boolean tryLock() {
        try {
            zkClient.createPersistent(Z_NODE);
            return true;
        }catch (ZkNodeExistsException e){
            return false;
        }
    }

    public void unlock() {
        zkClient.delete(Z_NODE);
    }

    public void waitForLock(){
        System.out.println("加锁失败");
        IZkDataListener listener = new IZkDataListener() {
            public void handleDataChange(String s, Object o) throws Exception {

            }

            public void handleDataDeleted(String s) throws Exception {
                System.out.println("唤醒");
                cdl.countDown();
            }
        };
        // 监听
        zkClient.subscribeDataChanges(Z_NODE, listener);
        if (zkClient.exists(Z_NODE)) {
            try {
                cdl.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // 释放监听
        zkClient.unsubscribeDataChanges(Z_NODE, listener);
    }

    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    public void lockInterruptibly() throws InterruptedException {

    }

    public Condition newCondition() {
        return null;
    }
}
