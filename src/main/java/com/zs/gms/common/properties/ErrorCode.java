package com.zs.gms.common.properties;

import lombok.Data;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SpringBootConfiguration
@PropertySource(value = {"classpath:properties/errorCode.properties"},encoding = "gbk")
@ConfigurationProperties(prefix = "errorcode")
@Data
public class ErrorCode {
    private Map<String,String> dispatch=new HashMap<>();
    private Map<String,String> map=new HashMap<>();
}

