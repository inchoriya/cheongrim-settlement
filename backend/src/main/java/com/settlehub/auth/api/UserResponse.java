package com.settlehub.auth.api;

import com.settlehub.organization.domain.UserAccount;
import com.settlehub.organization.domain.UserRole;

public record UserResponse(
        Long id,
        String email,
        String name,
        UserRole role,
        Long agencyId,
        Long merchantId
) {

    public static UserResponse from(UserAccount account) {
        return new UserResponse(
                account.getId(),
                account.getEmail(),
                account.getName(),
                account.getRole(),
                account.getAgency() == null ? null : account.getAgency().getId(),
                account.getMerchant() == null ? null : account.getMerchant().getId()
        );
    }
}
