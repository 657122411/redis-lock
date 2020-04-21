package com.tjh.redislock;

import org.redisson.Redisson;
import org.redisson.config.Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RedisLockApplication {

    @Bean
    public Redisson redisson(){
        //此为单机模式
        Config config = new Config();
        config.useSingleServer().setAddress("redis://localhost:6369").setDatabase(0);
        return (Redisson) Redisson.create(config);
    }

    public static void main(String[] args) {
        SpringApplication.run(RedisLockApplication.class, args);
    }

}
