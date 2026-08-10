package com.settlehub.policy.api;

import com.settlehub.policy.domain.FeePolicy;

import java.time.LocalDateTime;

public record FeePolicyResponse(
        Long id,
        Long agencyId,
        String name,
        int platformFeeBps,
        int agencyFeeBps,
        long riderFee,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        boolean active
) {

    public static FeePolicyResponse from(FeePolicy policy) {
        return new FeePolicyResponse(
                policy.getId(),
                policy.getAgency() == null ? null : policy.getAgency().getId(),
                policy.getName(),
                policy.getPlatformFeeBps(),
                policy.getAgencyFeeBps(),
                policy.getRiderFee(),
                policy.getEffectiveFrom(),
                policy.getEffectiveTo(),
                policy.isActive()
        );
    }
}
