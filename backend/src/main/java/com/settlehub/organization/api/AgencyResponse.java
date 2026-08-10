package com.settlehub.organization.api;

import com.settlehub.organization.domain.Agency;

public record AgencyResponse(
        Long id,
        String code,
        String name,
        boolean active
) {

    public static AgencyResponse from(Agency agency) {
        return new AgencyResponse(agency.getId(), agency.getCode(), agency.getName(), agency.isActive());
    }
}
