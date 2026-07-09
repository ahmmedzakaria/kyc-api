package com.nexacore.systemmodule.clientaccess.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.systemmodule.clientaccess.dto.ApiRegistryDto;
import com.nexacore.systemmodule.clientaccess.dto.ApiRegistryRequestDto;
import com.nexacore.systemmodule.clientaccess.service.interfaces.ClientApiRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/system/api-registry")
public class ApiRegistryController {

    private final ClientApiRegistryService clientApiRegistryService;

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<ApiRegistryDto>> save(@RequestBody ApiRegistryRequestDto requestDto,
                                                            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                clientApiRegistryService.save(requestDto, authentication.getName()),
                "API registry saved"
        ));
    }

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<ApiRegistryDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                clientApiRegistryService.list(),
                "API registry loaded"
        ));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<List<ApiRegistryDto>>> sync(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                clientApiRegistryService.syncFromAnnotations(authentication.getName()),
                "API registry synchronized"
        ));
    }
}
