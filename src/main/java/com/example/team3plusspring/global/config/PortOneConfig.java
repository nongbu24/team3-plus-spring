package com.example.team3plusspring.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class PortOneConfig {

    private final PortOneProperties properties;

    @Bean
    public RestClient portOneRestClient() {
        // PortOne API 호출 시 사용할 요청 설정 객체를 생성하고 timeout을 지정
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);  // 서버 연결 대기 시간: 3초
        requestFactory.setReadTimeout(5000);     // 응답 수신 대기 시간: 5초

        return RestClient.builder()
                .requestFactory(requestFactory) // 위에서 설정한 timeout 적용
                .baseUrl(properties.getBaseUrl()) // PortOne API 기본 URL
                .defaultHeader("Authorization", "PortOne " + properties.getApiSecret()) // 모든 요청에 인증 헤더 추가
                .build();
    }
}
