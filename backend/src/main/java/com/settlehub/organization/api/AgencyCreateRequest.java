package com.settlehub.organization.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgencyCreateRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 200) String name
) {
}
