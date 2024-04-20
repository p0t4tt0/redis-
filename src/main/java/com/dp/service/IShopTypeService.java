package com.dp.service;

import com.dp.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopTypeService extends IService<ShopType> {


    /**
     * 查询商户类型
     * @return
     */
    List<ShopType> queryList();
}
