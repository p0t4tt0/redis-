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
## 6.解决秒杀多线程并发导致的超卖问题
### 两种方案：
### --悲观锁：给线程加锁，让所有线程串行执行，性能差，无线程安全问题
### --乐观锁：不加锁，更新前通过判断是否有线程已经做了修改从而决定是否修改，可以通过添加版本号字段或者比较库存前后值即CAS法决定是否修改，性能高但线程执行成功率低，可以通过判断库存是否大于零而不是等于查询时库存值来提高成功率。
## 7.用户一人一单功能，使用悲观锁synchronized处理用户下单操作，防止一个用户多次下单，要注意锁的是每个用户的id并且范围应该锁住整个事物,这就涉及的spring事物失效问题，需要获取代理对象实现
## DAY5
## 1.redis分布式锁
### 多台服务器集群处理高并发请求时，同一个用户的多个请求会被负载均衡到不同tomcat服务器，从而由不同jvm中的线程处理，导致不同请求获取到了不同的锁，每个请求都可访问数据库带来线程安全问题，因此采用分布式锁给不同服务器建立一个唯一的锁
### redis可以通过set nx ex实现；
## 2.解决分布式锁锁误删问题
### 线程获取锁后可能发生长时间阻塞导致锁过期释放，这时其他线程获取到锁后执行业务，此时阻塞线程唤醒并释放了锁，那么这次释放的锁就是其他线程的锁，从而导致安全问题，因此在释放锁前应对线程标识做判断。

## 3.解决释放锁操作的原子性问题
### 释放锁操作涉及两个步骤，第一步是判断线程标识，第二部是释放锁，如果第一步判断成立，还没执行第二步时，jvm虚拟机执行垃圾清理强行阻塞进程，导致锁超时释放，就会造成如上重复释放误删问题，因此必须确保两步操作的原子性，可以采用lua脚本实现，redis可通过EVAL字段
### 调用lua脚本，只需在lua脚本中完成这两布即可。

## 4.redisson 代替自定义分布式锁
### redisson 是一个基于redis的更完善的分布式锁组件，可以导入依赖并进行配置后直接使用。
### redisson 具有可重入锁功能，同一个线程中，方法a获取了锁后调用另一个方法b，方法b同样请求锁，而此时锁被a使用中，b无法获取，从而b无法完成，而a也阻塞在b方法上，无法释放锁，造成死锁问题，redisson可以让b获取到锁，其原理是用redis的hash结构存储锁，key为线程标识，value为重入次数，当a获取锁时，判断是否存在该线程标识的锁，不存在则获取锁并设置线程标识和重入次数为1，同时设置过期时间；调用b方法后，b请求锁判断线程标识为true，然后将锁的value加一，重置过期时间；释放锁时，判断标识为true，然后将value减一，此时判断value是否为零，不为零说明不是最外层方法，不用删除锁，重置过期时间后返回，当为0时，则删除锁。
### redisson实现获取锁可重试性，利用信号量机制和redis的pubsub实现，线程如果没有获取到锁会进行等待，等待释放锁的信号，当收到信号后会重新尝试获取锁，如果又没获取到，则继续等待，如果等待时间超时则获取锁失败。（不会过多占用cpu）
### redisson超时续约，利用看门狗机制，当获取到锁后，开启定时任务，每隔一定时间重置锁有效时间，防止业务未执行完锁就被释放掉。

### redisson的multilock可以解决redis主从一致性问题，其原理是将多个redis设置为独立的节点，当线程请求获取锁时，必须获取到每个节点的可重入锁才算获取锁成功，这样，假如某个redis节点宕机了，其他线程无法获取足够数量的可重入锁，那么其他线程也无法获取锁成功，从而避免线程安全问题。

## DAY6
## 1.实现秒杀下单异步下单
## 将秒杀业务分成两步，先用redis完成库存容量、一人一单的判断（使用lua脚本）,再将下单业务放到阻塞队列中利用独立线程实现异步下单，最后使用jmeter进行压测，存在阻塞队列的jvm内存限制问题和jvm无数据持久化导致的数据安全问题

