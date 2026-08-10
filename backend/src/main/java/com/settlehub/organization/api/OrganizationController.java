package com.settlehub.organization.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping("/agencies")
    public ApiResponse<AgencyResponse> createAgency(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody AgencyCreateRequest request
    ) {
        return ApiResponse.ok(organizationService.createAgency(authUser, request));
    }

    @GetMapping("/agencies")
    public ApiResponse<List<AgencyResponse>> listAgencies(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return ApiResponse.ok(organizationService.listAgencies(authUser, activeOnly));
    }

    @PatchMapping("/agencies/{id}")
    public ApiResponse<AgencyResponse> updateAgency(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @Valid @RequestBody AgencyUpdateRequest request
    ) {
        return ApiResponse.ok(organizationService.updateAgency(authUser, id, request));
    }

    @PostMapping("/merchants")
    public ApiResponse<MerchantResponse> createMerchant(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody MerchantCreateRequest request
    ) {
        return ApiResponse.ok(organizationService.createMerchant(authUser, request));
    }

    @GetMapping("/merchants")
    public ApiResponse<List<MerchantResponse>> listMerchants(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(organizationService.listMerchants(authUser));
    }

    @PatchMapping("/merchants/{id}")
    public ApiResponse<MerchantResponse> updateMerchant(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long id,
            @Valid @RequestBody MerchantUpdateRequest request
    ) {
        return ApiResponse.ok(organizationService.updateMerchant(authUser, id, request));
    }
}
