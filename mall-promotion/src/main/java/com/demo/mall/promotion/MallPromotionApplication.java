package com.demo.mall.promotion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.demo.mall.promotion.mapper")
@SpringBootApplication(scanBasePackages = "com.demo.mall")
public class MallPromotionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallPromotionApplication.class, args);
    }
}
