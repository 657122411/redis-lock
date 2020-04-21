package com.tjh.redislock;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
public class IndexController {
    @Autowired
    private Redisson redisson;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @RequestMapping("/reduce_stock")
    public String reduceStock() {
        //@2: setnx
        String lockKey = "lockKey";
        try{
            //Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey,"locked");
            //为避免JVM宕机 进程被杀暴力运维，设置加锁时间
            //stringRedisTemplate.expire(lockKey,10, TimeUnit.SECONDS);
            //上两行仍可能不是原子性操作，应当使用
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey,"locked",10,TimeUnit.SECONDS);

            if(!result){
                return "error_code";
            }

            //@1： JVM层级的锁在分布式架构中无法阻挡高并发
            //synchronized (this) {
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", String.valueOf(realStock));
                System.out.println("扣减成功，剩余库存：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
            //}
        }finally {
            stringRedisTemplate.delete(lockKey);
        }

        return "end";
    }


    /**
     * 方法1中若线程2在线程1对Redis加锁成功后执行业务时进入，此时线程1执行delete key 造成逻辑混乱
     * @return
     */
    @RequestMapping("/reduce_stock2")
    public String reduceStock2() {
        //@2: setnx
        String lockKey = "lockKey";

        //拼接上线程唯一的标志避免方法一的漏洞
        String clientID = UUID.randomUUID().toString();
        try{
            //Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey,"locked");
            //为避免JVM宕机 进程被杀暴力运维，设置加锁时间
            //stringRedisTemplate.expire(lockKey,10, TimeUnit.SECONDS);
            //上两行仍可能不是原子性操作，应当使用
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey,clientID,10,TimeUnit.SECONDS);

            if(!result){
                return "error_code";
            }

            //@3： 如果业务执行时间确实过长，需要考虑锁续命！

            //@1： JVM层级的锁在分布式架构中无法阻挡高并发
            //synchronized (this) {
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", String.valueOf(realStock));
                System.out.println("扣减成功，剩余库存：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
            //}
        }finally {
            if(clientID.equals(stringRedisTemplate.opsForValue().get(lockKey))){
                stringRedisTemplate.delete(lockKey);
            }
        }

        return "end";
    }

    /**
     * 使用redisson框架 实现的分布式锁
     * @return
     */
    @RequestMapping("/reduce_stock3")
    public String reduceStock3() {
        //@2: setnx
        String lockKey = "lockKey";

        RLock redissonLock = redisson.getLock(lockKey);

        try{
            //加锁 :底层效果同上  内有锁续命
            redissonLock.lock();
            //内部Redis执行的是lua脚本 保证一致性

            //@1： JVM层级的锁在分布式架构中无法阻挡高并发
            //synchronized (this) {
            int stock = Integer.parseInt(stringRedisTemplate.opsForValue().get("stock"));
            if (stock > 0) {
                int realStock = stock - 1;
                stringRedisTemplate.opsForValue().set("stock", String.valueOf(realStock));
                System.out.println("扣减成功，剩余库存：" + realStock);
            } else {
                System.out.println("扣减失败，库存不足");
            }
            //}
        }finally {
            redissonLock.unlock();
        }

        return "end";
    }


    //另有zookeeper (以及不推荐使用的Redlock :超过半数的redis节点加锁成功)


}
