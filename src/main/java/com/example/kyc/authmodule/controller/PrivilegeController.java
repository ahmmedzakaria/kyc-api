package com.example.kyc.authmodule.controller;

import com.example.kyc.authmodule.dto.PrivilegeAssignmentRequestDto;
import com.example.kyc.authmodule.dto.PrivilegeCheckRequestDto;
import com.example.kyc.authmodule.dto.PrivilegeCheckResponseDto;
import com.example.kyc.authmodule.dto.PrivilegeDto;
import com.example.kyc.authmodule.dto.PrivilegeRequestDto;
import com.example.kyc.authmodule.service.interfaces.PrivilegeService;
import com.example.kyc.commonmodule.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/privilege")
public class PrivilegeController {

    private final PrivilegeService privilegeService;

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<PrivilegeDto>> savePrivilege(@RequestBody PrivilegeRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(privilegeService.savePrivilege(requestDto), "Privilege saved"));
    }

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<PrivilegeDto>>> listPrivileges() {
        return ResponseEntity.ok(ApiResponse.success(privilegeService.getAllPrivileges(), "Privileges loaded"));
    }

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<PrivilegeCheckResponseDto>> checkPrivilege(@RequestBody PrivilegeCheckRequestDto requestDto,
                                                                                 Authentication authentication) {
        if (requestDto.getUsername() == null || requestDto.getUsername().isBlank()) {
            requestDto.setUsername(authentication.getName());
        }
        return ResponseEntity.ok(ApiResponse.success(privilegeService.checkPrivilege(requestDto), "Privilege checked"));
    }

    @PostMapping("/my-codes")
    public ResponseEntity<ApiResponse<Set<String>>> myPrivilegeCodes(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.getUserPrivilegeCodes(authentication.getName()),
                "User privileges loaded"
        ));
    }

    @PostMapping("/assign-role")
    public ResponseEntity<ApiResponse<Void>> assignPrivilegesToRole(@RequestBody PrivilegeAssignmentRequestDto requestDto) {
        privilegeService.assignPrivilegesToRole(requestDto);
        return ResponseEntity.ok(ApiResponse.success(null, "Role privileges updated"));
    }

    @PostMapping("/assign-user")
    public ResponseEntity<ApiResponse<Void>> assignPrivilegesToUser(@RequestBody PrivilegeAssignmentRequestDto requestDto) {
        privilegeService.assignPrivilegesToUser(requestDto);
        return ResponseEntity.ok(ApiResponse.success(null, "User privileges updated"));
    }
}
