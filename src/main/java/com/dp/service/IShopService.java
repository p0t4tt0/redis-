package com.dp.service;

import com.dp.dto.Result;
import com.dp.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopService extends IService<Shop> {

    /**
     * 查询商户信息
     * @param id
     * @return
     */
    Object queryById(Long id);

    /**
     * 更新商店
     * @param shop
     * @return
     */
    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
