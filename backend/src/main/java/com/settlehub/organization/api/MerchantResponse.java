package com.settlehub.organization.api;

import com.settlehub.organization.domain.Merchant;

public record MerchantResponse(
        Long id,
        Long agencyId,
        String agencyCode,
        String code,
        String name,
        boolean active
) {

    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getAgency().getId(),
                merchant.getAgency().getCode(),
                merchant.getCode(),
                merchant.getName(),
                merchant.isActive()
        );
    }
}
