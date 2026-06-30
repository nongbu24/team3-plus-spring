package com.example.team3plusspring.domain.portone.controller;

import com.example.team3plusspring.domain.portone.dto.PortOneConfigResponse;
import com.example.team3plusspring.global.config.PortOneProperties;
import com.example.team3plusspring.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PortOneConfigController {

    private final PortOneProperties portOneProperties;

    @GetMapping("/api/config/portone")
    public ResponseEntity<ApiResponse<PortOneConfigResponse>> getConfig() {
        PortOneConfigResponse response = PortOneConfigResponse.of(
                portOneProperties.getStoreId(),
                portOneProperties.getChannelKey()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
