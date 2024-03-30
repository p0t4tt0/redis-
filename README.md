# redis实战项目-类大众点评app
## DAY1
## 1.短信验证码发送功能实现
## 2.用户短信登陆、拦截器部署
### 用户登录请求传递cookie，cookie中携带session id，session中保存了用户信息，通过session中保存的用户信息，可以在前置拦截器中校验登录信息从而显示页面，
### 并将用户信息保存到threadlocal中，从而在controller层中获取到该登录用户并返回完成校验，最后在后置拦截其中销毁threadlocal中的user值从而避免内存泄漏；
## DAY2
## 1.用redis代替session实现短音登录功能从而解决session集群共享问题；对拦截器进一步优化：差分为token刷新拦截器和登录校验拦截器
## 2.添加数据访问redis缓存--商店详情页面查询、首页商店类型查询

## DAY3
## 1.redis缓存更新策略-先更新数据库再删除缓存，保证事物一致性
## 2.redis缓存穿透问题被动解决方案-缓存空值法
## 3.缓存雪崩
## --同一时间段内大量key同时失效：为不同key设置随机ttl
## --redis宕机：利用redis集群哨兵机制、缓存业务添加降级限流策略
## 4.缓存击穿（热点key问题）
### 某个 被高并发访问 并且 缓存重建业务较复杂 的key突然失效，无数请求访问瞬间冲击数据库
## 解决方案：
## --互斥锁：当需要修改缓存时，请求锁，没请求到的线程等待，修改完成后释放锁，保证一致性，实现简单，无额外内存消耗，但牺牲了性能，有死锁风险
## --逻辑过期：不给热点缓存设置ttl，而是逻辑上添加一个过期值，实际上永不过期，线程查询缓存时只需判断逻辑时间是否过期，如果过期则请求锁，同时创建一个新线程
## 去重建缓存，完成后释放锁，而原先的线程继续返回旧的缓存值，其它未获得锁的线程也返回旧值，性能较好，但不保证一致性，有额外内存消耗


## DAY4
## 1.互斥锁解决缓存击穿，使用JMeter进行多线程并发访问压测
## 2.逻辑过期解决缓存击穿
## 3.缓存相关set、get方法封装为工具类
## 4.基于redis自增实现全局唯一Id，由时间戳+redis自增+当前日期区分key位运算拼接成的64位long型id
## 5.优惠券添加、秒杀优惠券下单功能实现
