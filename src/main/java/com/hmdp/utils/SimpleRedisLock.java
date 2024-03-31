package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements Ilock{

    private StringRedisTemplate stringRedisTemplate;
    private String name;
    private static final String KEY_PREFIX ="lock:";

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(long timeoutsec) {

        //set nx ex

        //获取线程id
        long threadId = Thread.currentThread().getId();
        //获取锁
        String key=KEY_PREFIX+name;

        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, threadId + "", timeoutsec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);//防止自动拆箱造成空指针
    }

    @Override
    public void unlock() {

        stringRedisTemplate.delete(KEY_PREFIX+name);

    }
}
