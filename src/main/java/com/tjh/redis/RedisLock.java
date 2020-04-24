package com.tjh.redis;

import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;

@Component
public class RedisLock {

    private String LOCK_KEY = "redis_lock";

    protected long INTERNAL_LOCK_LEASE_TIME = 10;

    private long timeout = 1000;

    private SetParams params = SetParams.setParams().nx().px(INTERNAL_LOCK_LEASE_TIME);

    JedisPool jedisPool = new JedisPool("127.0.0.1", 6379);

    /**
     * 加锁
     *
     * @param id
     * @return
     */
    public boolean lock(String id) {
        Long start = System.currentTimeMillis();
        Jedis jedis = jedisPool.getResource();
        try {
            for (; ; ) {
                //SET命令返回OK ，则证明获取锁成功
                String lock = jedis.set(LOCK_KEY, id, params);
                if ("OK".equals(lock)) {
                    return true;
                }
                //否则循环等待，在timeout时间内仍未获取到锁，则获取失败
                long l = System.currentTimeMillis() - start;
                if (l >= timeout) {
                    return false;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            jedis.close();
        }
    }

    /**
     * 解锁
     *
     * @param id
     * @return
     */
    public boolean unlock(String id) {
        Jedis jedis = jedisPool.getResource();
        String script =
                "if redis.call('get',KEYS[1]) == ARGV[1] then" +
                        "   return redis.call('del',KEYS[1]) " +
                        "else" +
                        "   return 0 " +
                        "end";
        try {
            String result = jedis.eval(script, Collections.singletonList(LOCK_KEY), Collections.singletonList(id)).toString();
            return "1".equals(result) ? true : false;
        } finally {
            jedis.close();
        }
    }
}