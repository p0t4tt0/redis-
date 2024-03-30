package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.events.Event;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_NULL_TTL;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

@Component
@Slf4j
public class CacheClient {
    @Resource
    private StringRedisTemplate stringRedisTemplate;



    public void set(String key, Object value, Long time, TimeUnit timeUnit)
    {



        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,timeUnit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit timeUnit)
    {
        //设置逻辑过期
        RedisData redisData=new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));

        //写入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }
    /**
     * 解决缓存穿透
     * @return
     */
    public <R,ID> R queryWithPassThrough(String keyPre, ID id, Class<R> type, Function<ID,R> dbFallBack,Long time, TimeUnit timeUnit)
    {
        //从redis查询商户缓存

        String key=keyPre + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在

        if (StrUtil.isNotBlank(json)) {

            //存在，返回

           return JSONUtil.toBean(json, type);//反序列化为shop对象


        }

        //命中是否为空值
        if (json!=null)
        {

            //返回错误
            return null;

        }


        //不存在，查询数据库

        R r= dbFallBack.apply(id);



        //不存在，错误
        if(r==null)
        {
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);

            //返回错误
            return null;

        }

        //存在，写入缓存

        this.set(key,r,time,timeUnit);

        //返回数据
        return r;
    }


    private final static ExecutorService CATCHE_REBUILD_EXCUTOR= Executors.newFixedThreadPool(10);//新建线程池


    public <R,ID> R queryWithLogicalExpire(String keyPreFix,ID id,Class<R> type,Function<ID,R> dbfallback,Long time, TimeUnit timeUnit)
    {
        //从redis查询商户缓存

        String key=RedisConstants.CACHE_SHOP_KEY + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在

        if (StrUtil.isBlank(json)) {


//不存在返回null
            return null;
        }


        //redis命中，反序列化未对象

        RedisData redisData= JSONUtil.toBean(json,RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        R r = JSONUtil.toBean(data, type);
        LocalDateTime expireTime = redisData.getExpireTime();


        //判断是否过期


        if (expireTime.isAfter(LocalDateTime.now())) {
            //未过期，返回数据

            return r;
        }
        //过期，缓存建

        String lockKey=LOCK_SHOP_KEY+id;
        //获取互斥锁

        boolean islock = tryGetLock(lockKey);
        if(islock) {
            //成功，新建线程重建

            CATCHE_REBUILD_EXCUTOR.submit(()->{
                //重建缓存

                try {
                    //重建缓存

                    //查数据库

                    R r1 = dbfallback.apply(id);

                    //写入redis

                    setWithLogicalExpire(key,r1,time,timeUnit);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {

                    //释放锁
                    unlock(lockKey);

                }



            });

        }
        //返回过期数据
        return r;
    }


    //获取锁
    private  boolean tryGetLock(String key)
    {
        Boolean flag=stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);


    }

    //释放锁
    private void unlock(String key)
    {
        stringRedisTemplate.delete(key);
    }
}
