package com.hmdp.utils;


import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {

    //初始时间戳
    private static final long BEGIN_TIMESTAMP=1711756800L;
    //序列号的位数
    private static final int COUNT_BITS = 32;


    @Resource
    private StringRedisTemplate stringRedisTemplate;
    public Long nextId(String keyPrefix)
    {
        //生成时间戳 当前时间-初始时间


        LocalDateTime now = LocalDateTime.now();
        long nowl = now.toEpochSecond(ZoneOffset.UTC);
        Long timeStamp=nowl-BEGIN_TIMESTAMP;


        //生成序列号

        //获取当前日期

        String yyyyMMdd = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));//精确到每天
        //自增长
       long count= stringRedisTemplate.opsForValue().increment("icr:"+keyPrefix+":"+yyyyMMdd);

        //拼接返回
        return timeStamp << COUNT_BITS | count;//位运算
    }


}
