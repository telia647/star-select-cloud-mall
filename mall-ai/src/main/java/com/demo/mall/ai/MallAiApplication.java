package com.demo.mall.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@MapperScan("com.demo.mall.ai.mapper")
@SpringBootApplication(scanBasePackages = "com.demo.mall", exclude = MilvusVectorStoreAutoConfiguration.class)
public class MallAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallAiApplication.class, args);
    }
}
