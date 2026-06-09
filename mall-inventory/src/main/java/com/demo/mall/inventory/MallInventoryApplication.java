package com.demo.mall.inventory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.demo.mall.inventory.mapper")
@SpringBootApplication(scanBasePackages = "com.demo.mall")
public class MallInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallInventoryApplication.class, args);
    }
}
