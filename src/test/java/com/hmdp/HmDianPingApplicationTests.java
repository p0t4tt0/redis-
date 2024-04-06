package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SystemConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class HmDianPingApplicationTests {

@Resource
    private ShopServiceImpl shopService;

@Resource
private CacheClient cacheClient;

@Autowired
private RedisIdWorker redisIdWorker;

@Resource
private StringRedisTemplate stringRedisTemplate;


private ExecutorService executorService= Executors.newFixedThreadPool(500);
@Test
    void testShopSave() throws InterruptedException {
    shopService.saveShopToRedis(1L,10L);
}

@Test
    void testCacheUtils()
{

    Shop shop = shopService.getById(1L);
    cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY+1L,shop,10L, TimeUnit.SECONDS);
}

@Test
    void testIdWorker() throws InterruptedException {
    CountDownLatch countDownLatch=new CountDownLatch(300);

    Runnable task=()->{
        for (int i=0;i<100;i++)
        {

            Long id = redisIdWorker.nextId("orders");
            System.out.println("id="+id);
        }
        countDownLatch.countDown();
    };

    long begin=System.currentTimeMillis();
    for (int i=0;i<300;i++)
    {

        executorService.submit(task);
    }

    countDownLatch.await();

    long end=System.currentTimeMillis();

    System.out.println("time = "+(end-begin));
}

@Test
    void loadshopdata()
{

    //查询商铺信息

    List<Shop> shops = shopService.list();


    //将商铺按type分组

    Map<Long, List<Shop>> shopgroup = shops.stream().collect(Collectors.groupingBy(Shop::getTypeId));

    //分批写入redis

    for (Map.Entry<Long,List<Shop>> entry:shopgroup.entrySet())
    {

        Long typeId = entry.getKey();

        List<Shop> value = entry.getValue();
        String key= RedisConstants.SHOP_GEO_KEY+typeId;

      List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());

        for (Shop shop : value) {


            locations.add(new RedisGeoCommands.GeoLocation<>(shop.getId().toString(),new Point(shop.getX(), shop.getY())));
        }
        stringRedisTemplate.opsForGeo().add(key,locations);
    }

}
}
