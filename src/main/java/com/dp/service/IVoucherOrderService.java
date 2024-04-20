package com.dp.service;

import com.dp.dto.Result;
import com.dp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀优惠券
     * @param voucherId
     * @return
     */
    Result seckillvourcher(Long voucherId);

    void createVoucher(VoucherOrder voucherOrder);
}
