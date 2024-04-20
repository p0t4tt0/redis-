package com.dp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements Ilock{

    private StringRedisTemplate stringRedisTemplate;
    private String name;
    private static final String KEY_PREFIX ="lock:";
    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"_";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(long timeoutsec) {

        //set nx ex

        //获取线程标识

        long threadId = Thread.currentThread().getId();
        String threadIC=ID_PREFIX+threadId;
        //获取锁
        String key=KEY_PREFIX+name;

        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, threadIC + "", timeoutsec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);//防止自动拆箱造成空指针
    }

    @Override
    public void unlock() {


        /*String key=KEY_PREFIX+name;
        long threadId = Thread.currentThread().getId();
        String threadIC=ID_PREFIX+threadId;
        //锁中标示
        String ti = stringRedisTemplate.opsForValue().get(key);

        //判断标识是否一致
        if (ti.equals(threadIC)) {
            stringRedisTemplate.delete(KEY_PREFIX + name);

        }*/

        //调用lua脚本
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(KEY_PREFIX+name),ID_PREFIX+Thread.currentThread().getId());

    }
}
