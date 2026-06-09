package com.demo.mall.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.inventory.MallInventoryApplication;
import com.demo.mall.inventory.dto.StockItemRequest;
import com.demo.mall.inventory.dto.StockLockRequest;
import com.demo.mall.inventory.entity.Stock;
import com.demo.mall.inventory.entity.StockFlow;
import com.demo.mall.inventory.entity.StockLock;
import com.demo.mall.inventory.mapper.StockFlowMapper;
import com.demo.mall.inventory.mapper.StockLockMapper;
import com.demo.mall.inventory.mapper.StockMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = MallInventoryApplication.class, properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class StockServiceIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.0")
            .withDatabaseName("mall_inventory")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private StockService stockService;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private StockLockMapper stockLockMapper;

    @Autowired
    private StockFlowMapper stockFlowMapper;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void lockReleaseAndDeductPersistStockFlows() {
        stockService.lock(new StockLockRequest("IT1001", List.of(new StockItemRequest(3001L, 2))));

        Stock locked = stock(3001L);
        assertThat(locked.getAvailableStock()).isEqualTo(98);
        assertThat(locked.getLockedStock()).isEqualTo(2);
        assertThat(flowCount("IT1001")).isEqualTo(1);

        stockService.release("IT1001");

        Stock released = stock(3001L);
        assertThat(released.getAvailableStock()).isEqualTo(100);
        assertThat(released.getLockedStock()).isEqualTo(0);
        assertThat(lockStatus("IT1001", 3001L)).isEqualTo(2);
        assertThat(flowCount("IT1001")).isEqualTo(2);

        stockService.lock(new StockLockRequest("IT1002", List.of(new StockItemRequest(3001L, 2))));
        stockService.deduct("IT1002");

        Stock deducted = stock(3001L);
        assertThat(deducted.getAvailableStock()).isEqualTo(98);
        assertThat(deducted.getLockedStock()).isEqualTo(0);
        assertThat(lockStatus("IT1002", 3001L)).isEqualTo(3);
        assertThat(flowCount("IT1002")).isEqualTo(2);
    }

    private Stock stock(Long skuId) {
        return stockMapper.selectOne(new LambdaQueryWrapper<Stock>()
                .eq(Stock::getSkuId, skuId));
    }

    private Integer lockStatus(String orderNo, Long skuId) {
        StockLock stockLock = stockLockMapper.selectOne(new LambdaQueryWrapper<StockLock>()
                .eq(StockLock::getOrderNo, orderNo)
                .eq(StockLock::getSkuId, skuId));
        return stockLock.getStatus();
    }

    private Long flowCount(String orderNo) {
        return stockFlowMapper.selectCount(new LambdaQueryWrapper<StockFlow>()
                .eq(StockFlow::getOrderNo, orderNo));
    }
}
