package com.settlehub.policy.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record FeePolicyRequest(
        Long agencyId,
        @NotBlank String name,
        @NotNull @Min(0) @Max(10000) Integer platformFeeBps,
        @NotNull @Min(0) @Max(10000) Integer agencyFeeBps,
        @NotNull @Min(0) Long riderFee,
        @NotNull LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo
) {
}
