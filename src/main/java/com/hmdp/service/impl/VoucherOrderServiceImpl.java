package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT=new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    //阻塞队列
    private BlockingQueue<VoucherOrder> orderTask=new ArrayBlockingQueue<>(1024*1024);


    //线程池
    private static  final  ExecutorService SECKILL_ORDER_EXCUTER= Executors.newSingleThreadExecutor();

    private IVoucherOrderService proxy;


    @PostConstruct//当前类初始化完毕后立刻执行
    private void init(){

        SECKILL_ORDER_EXCUTER.submit(new VorcherOrderHandler());
    }

    //内部类实现线程任务
    private class VorcherOrderHandler implements Runnable{
        @Override
        public void run() {


            while (true)
            {



                try {

                    //获取队列里订单信息
                    VoucherOrder order = orderTask.take();

                    //创建订单
                    handleVorcherOrder(order);
                } catch (Exception e) {
                    log.error("处理订单异常：{}",e);
                }

            }

        }


    }

    private void handleVorcherOrder(VoucherOrder order) {


        //获取用户
        Long userId = order.getUserId();
        //创建锁对象，获取锁
        // SimpleRedisLock simpleRedisLock = new SimpleRedisLock(stringRedisTemplate, "order" + userid);

        RLock lock = redissonClient.getLock("lock:order:" +userId );

        boolean islock = lock.tryLock();//默认等待时间-1，超时时间30s
        if(!islock)
        {

            //获取失败
            log.error("不允许重发下单");
            return;

        }
        //获取代理对象

        try {
            //返回订单
             proxy.createVoucher(order);
        }
        finally {
            lock.unlock();
        }

    }


    @Override

    public Result seckillvourcher(Long voucherId) {

        Long userId = UserHolder.getUser().getId();
        //执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                String.valueOf(voucherId),
                String.valueOf(userId)
        );

        //判断结果是否为零
        if(result.intValue()!=0)
        {
            //不为0，无购买资格
            return Result.fail(result.intValue()==1?"库存不足":"不能重复下单");
        }





        //为零，有购买资格，下单信息保存到阻塞队列

        //创建订单

        VoucherOrder voucherOrder = new VoucherOrder();

        //订单id
        Long orderId = redisIdWorker.nextId("order");

        voucherOrder.setId(orderId);
        //用户id


        voucherOrder.setUserId(userId);
        //代金券id

        voucherOrder.setVoucherId(voucherId);




        //创建阻塞队列

        orderTask.add(voucherOrder);

        //获取代理对象

        proxy = (IVoucherOrderService) AopContext.currentProxy();
        //返回订单id

        return Result.ok(orderId);

    }
   /* @Override

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

        /*给不同用户加锁，先获取锁，再修改数据库，再进行事物提交完成修改，最后释放锁，确保线程安全，但要注意确保spring事物生效（目标对象与代理对象）从而确保事物安全
        synchronized (userid.toString().intern()) {

            //获取代理对象
            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
            //返回订单
            return proxy.createVoucher(voucherId);

        }*/

       /* //创建锁对象，获取锁
       // SimpleRedisLock simpleRedisLock = new SimpleRedisLock(stringRedisTemplate, "order" + userid);

        RLock lock = redissonClient.getLock("lock:order:" + userid);

        boolean islock = lock.tryLock();//默认等待时间-1，超时时间30s
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
            lock.unlock();
        }


    }*/

    @Transactional//订单和优惠券两张表
    public void createVoucher(VoucherOrder voucherOrder)
    {
        //一人一单
        Long userid = voucherOrder.getUserId();



            //查询订单是否存在
            Integer ordercount = query().eq("user_id", userid).eq("voucher_id", voucherOrder.getVoucherId()).count();
            if (ordercount > 0) {
               log.error("用户已经购买过");
               return;
            }
            //扣减库存

            boolean isSC = seckillVoucherService.update().setSql("stock=stock-1")
                    .eq("voucher_id", voucherOrder.getVoucherId())
                    .gt("stock", 0)//乐观锁判断库存是否大于0
                    .update();
            if (!isSC) {

                log.error("库存不足");
                return;
            }

            save(voucherOrder);

    }
}
