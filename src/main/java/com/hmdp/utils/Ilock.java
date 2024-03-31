package com.hmdp.utils;

public interface Ilock {


    /**
     * 尝试获取锁
     * @param timeoutsec
     * @return
     */
    boolean tryLock(long timeoutsec);


    /**
     * 释放锁
     */
    void unlock();

}
