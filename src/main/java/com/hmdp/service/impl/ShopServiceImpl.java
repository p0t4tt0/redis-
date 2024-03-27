package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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

    /**
     * 查询商户信息
     * @param id
     * @return
     */

    public Object queryById(Long id) {
        //从redis查询商户缓存

        String key=RedisConstants.CACHE_SHOP_KEY + id;
        String shopjson = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在

        if (StrUtil.isNotBlank(shopjson)) {

            //存在，返回

            Shop shopbean = JSONUtil.toBean(shopjson, Shop.class);//反序列化为shop对象

            return Result.ok(shopbean);
        }



        //不存在，查询数据库

        Shop shop = getById(id);

        //不存在，错误
        if(shop==null)
        {
            return Result.fail("店铺不存在");
        }

        //存在，写入缓存

        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop));

        //返回数据
        return Result.ok(shop);
    }
}
