package com.settlehub.organization.api;

import jakarta.validation.constraints.Size;

public record AgencyUpdateRequest(
        @Size(max = 200) String name,
        Boolean isActive
) {
}
