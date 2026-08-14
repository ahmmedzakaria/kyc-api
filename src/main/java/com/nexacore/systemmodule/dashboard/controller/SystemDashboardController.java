package com.nexacore.systemmodule.dashboard.controller;
import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedApi;
import com.nexacore.systemmodule.dashboard.dto.DashboardAggregateDto;
import com.nexacore.systemmodule.dashboard.service.SystemDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/system/dashboard") @RequiredArgsConstructor
public class SystemDashboardController {
 private final SystemDashboardService service;
 @PostMapping("/aggregate") @AuthenticatedApi
 public ApiResponse<DashboardAggregateDto> aggregate(Authentication authentication){return ApiResponse.successCode(service.aggregate(authentication),"system.dashboard.aggregated","Authorized dashboard loaded");}
}
