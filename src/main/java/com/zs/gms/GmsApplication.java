package com.zs.gms;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ServletComponentScan
@MapperScan("com.zs.gms.mapper.**")
@EnableScheduling
@EnableTransactionManagement
public class GmsApplication {
    public static void main(String[] args) {
        /*System.setProperty("SERVER_HOST","192.168.2.100");
        System.setProperty("SERVER_PWD","123456");*/
        SpringApplication.run(GmsApplication.class, args);
    }
}
