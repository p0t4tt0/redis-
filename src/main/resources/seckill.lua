--优惠券id
local vorcherId=ARGV[1]
--用户id
local userId=ARGV[2]
--订单id
local orderId=ARGV[3]

--数据key
--库存key
local stockkey='seckill:stock:'..vorcherId
--订单key
local orderkey='seckill:order:'..vorcherId

--判断库存是否充足

if(tonumber(redis.call('get',stockkey))<=0)
    then
    --库存不足
return 1
end
--判断用户是否下单
if(redis.call('sismember',orderkey,userId)==1)then
    --重复下单
    return 2
end
--减库存、下单

redis.call('incrby',stockkey,-1)
redis.call('sadd',orderkey,userId)

--发送消息到队列中
redis.call('xadd','stream.orders','*','userId',userId,'voucherId',vorcherId,'id',orderId)

return 0
