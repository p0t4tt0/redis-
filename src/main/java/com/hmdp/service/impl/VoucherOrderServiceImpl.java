package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Override

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


        Long userid = UserHolder.getUser().getId();

        /*//给不同用户加锁，先获取锁，再修改数据库，再进行事物提交完成修改，最后释放锁，确保线程安全，但要注意确保spring事物生效（目标对象与代理对象）从而确保事物安全
        synchronized (userid.toString().intern()) {

            //获取代理对象
            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
            //返回订单
            return proxy.createVoucher(voucherId);

        }*/

        //创建锁对象，获取锁
        SimpleRedisLock simpleRedisLock = new SimpleRedisLock(stringRedisTemplate, "order" + userid);

        boolean islock = simpleRedisLock.tryLock(1200);
        if(!islock)
        {

            //获取失败
            return Result.fail("一人只允许下一单！");

        }
        //获取代理对象
        IVoucherOrderService proxy = null;
        try {
            proxy = (IVoucherOrderService) AopContext.currentProxy();
            //返回订单
            return proxy.createVoucher(voucherId);
        }
        finally {
            simpleRedisLock.unlock();
        }


    }

    @Transactional//订单和优惠券两张表
    public Result createVoucher(Long voucherId)
    {
        //一人一单
        Long userid = UserHolder.getUser().getId();



            //查询订单是否存在
            Integer ordercount = query().eq("user_id", userid).eq("voucher_id", voucherId).count();
            if (ordercount > 0) {
                return Result.fail("用户已经购买过");
            }
            //扣减库存

            boolean isSC = seckillVoucherService.update().setSql("stock=stock-1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)//乐观锁判断库存是否大于0
                    .update();
            if (!isSC) {

                return Result.fail("库存不足");
            }
            //创建订单

            VoucherOrder voucherOrder = new VoucherOrder();

            //订单id
            Long orderid = redisIdWorker.nextId("order");

            voucherOrder.setId(orderid);
            //用户id


            voucherOrder.setUserId(userid);
            //代金券id

            voucherOrder.setVoucherId(voucherId);

            save(voucherOrder);
            return Result.ok(voucherOrder);

    }
}
