package com.scenic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class ScenicApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScenicApplication.class, args);
        System.out.println("========================================");
        System.out.println("  景区门票销售及入场系统启动成功！");
        System.out.println("  API文档: http://localhost:8080/doc.html");
        System.out.println("========================================");
    }
}
