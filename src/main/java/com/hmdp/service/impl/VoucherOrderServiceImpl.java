package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    @Transactional//订单和优惠券两张表
    public Result seckillvourcher(Long voucherId) {

        //查询优惠券信息

        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //判断是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //未开始

            return Result.fail("秒杀尚未开始");
        }

        //判断是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            //已结束
            return Result.fail("秒杀已结束");
        }

        //判断库存

        if(voucher.getStock()<1)
        {

            //已抢完
            return Result.fail("库存不足");
        }

        //扣减库存

        boolean isSC = seckillVoucherService.update().setSql("stock=stock-1").eq("voucher_id", voucherId).update();
        if(!isSC)
        {

            return Result.fail("库存不足");
        }

        //创建订单

        VoucherOrder voucherOrder = new VoucherOrder();

        //订单id
        Long orderid = redisIdWorker.nextId("order");

        voucherOrder.setId(orderid);
        //用户id

        Long userid = UserHolder.getUser().getId();
        voucherOrder.setUserId(userid);
        //代金券id

        voucherOrder.setVoucherId(voucherId);

        save(voucherOrder);

        //返回订单
        return Result.ok(voucherOrder);
    }
}
