package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 查询商户类型
     * @return
     */
    public List<ShopType> queryList() {

        //查询redis
        String key= RedisConstants.SHOP_TYPE_KEY;
        Set<String> shopTypeSet = stringRedisTemplate.opsForZSet().range(key, 0, -1);

        log.debug("redis中商店类型：{}",shopTypeSet);
        if (shopTypeSet!=null&&shopTypeSet.size()>0) {

            List<ShopType> shopTypeList=new ArrayList<>();
            //存在，返回
            for(String s:shopTypeSet)
            {
                shopTypeList.add(JSONUtil.toBean(s,ShopType.class));

            }
            return shopTypeList;

        }


        //不存在，查询数据库

        List<ShopType> shopType = query().orderByAsc("sort").list();
        log.debug("商店类型：{}",shopType);

        //不存在，错误

        if(shopType==null)
        {
            return null;
        }

        //存在，写回redis

        for(ShopType shopType1:shopType)
        {

            String shopTypeJson = JSONUtil.toJsonStr(shopType1);

            stringRedisTemplate.opsForZSet().add(key,shopTypeJson,shopType1.getSort());

        }


        //将数据返回
        return shopType;
    }
}
