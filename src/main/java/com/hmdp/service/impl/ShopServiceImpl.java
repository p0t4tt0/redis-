package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    /**
     * 查询商户信息
     * @param id
     * @return
     */

    public Object queryById(Long id) {
       //缓存穿透
       // Shop shop = queryWithPassThrough(id);

        //互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);

        //逻辑过期解决缓存击穿

        //Shop shop = queryWithLogicalExpire(id);

        //使用工具类解决缓存穿透
       // Shop shop=cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

        Shop shop=cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY,id,Shop.class,this::getById,20L,TimeUnit.MINUTES);

        if (shop==null) {
            return Result.fail("店铺不存在");
        }
        //返回数据
        return Result.ok(shop);
    }



    /**
     * 解决缓存穿透
     * @return
     */
    //public Shop queryWithPassThrough(Long id)
   /* {
        //从redis查询商户缓存

        String key=RedisConstants.CACHE_SHOP_KEY + id;
        String shopjson = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在

        if (StrUtil.isNotBlank(shopjson)) {

            //存在，返回

            Shop shopbean = JSONUtil.toBean(shopjson, Shop.class);//反序列化为shop对象

            return shopbean;
        }

        //命中是否为空值
        if (shopjson!=null)
        {

            //返回错误
            return null;

        }


        //不存在，查询数据库

        Shop shop = getById(id);



        //不存在，错误
        if(shop==null)
        {
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);

            //返回错误
            return null;

        }

        //存在，写入缓存

        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        //返回数据
        return shop;
    }
*/
    /**
     * 解决缓存击穿--互斥锁
     * @return
     */
    public Shop queryWithMutex(Long id)
    {
        //从redis查询商户缓存

        String key=RedisConstants.CACHE_SHOP_KEY + id;
        String shopjson = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在

        if (StrUtil.isNotBlank(shopjson)) {

            //存在，返回

            Shop shopbean = JSONUtil.toBean(shopjson, Shop.class);//反序列化为shop对象

            return shopbean;
        }

        //命中是否为空值
        if (shopjson!=null)
        {

            //返回错误
            return null;

        }


        //不存在，查询数据库

        //获取互斥锁

        String lockKey=LOCK_SHOP_KEY+id;//每个店铺一个锁
        Shop shop = null;
        try {
            boolean isLock = tryGetLock(lockKey);
            if(!isLock) {
                //失败，休眠重试
                Thread.sleep(50);

               return queryWithMutex(id);

            }

            //成功写入redis

            shop = getById(id);

            //模拟重建延时
            Thread.sleep(200);
            //不存在，错误
            if(shop==null)
            {
                //将空值写入redis
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL,TimeUnit.MINUTES);

                //返回错误
                return null;

            }

            //存在，写入缓存

            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            //释放互斥锁

            unlock(lockKey);
        }




        //返回数据
        return shop;
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


    /**
     * 保存商店信息到redis
     */
    public void saveShopToRedis(Long id,Long expireSeconds) throws InterruptedException {

        //查询店铺数据
        Shop shop = getById(id);

        //模拟延迟
        Thread.sleep(200);

        //封装逻辑过期时间

        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));

    }
    /**
     * 更新商店信息
     * @param shop
     * @return
     */
    @Transactional //事务控制缓存和数据库的原子性
    public Result update(Shop shop) {

        //更新数据库
        Long id=shop.getId();
        if (id==null)
        {
            return Result.fail("商铺id不能为空");
        }
        String key=RedisConstants.CACHE_SHOP_KEY + id;
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(key);
        return Result.ok();
    }
}
