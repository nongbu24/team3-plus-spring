package com.example.team3plusspring.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "portone") // application.yml의 portone.* 설정 값을 이 클래스 필드에 바인딩
@Getter
@Setter
public class PortOneProperties {

    private String baseUrl;
    private String apiSecret;
    private String storeId;
    private String channelKey;
}
