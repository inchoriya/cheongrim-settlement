package com.settlehub.organization.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.exception.BusinessException;
import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.AgencyRepository;
import com.settlehub.organization.domain.Merchant;
import com.settlehub.organization.domain.MerchantRepository;
import com.settlehub.organization.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final AgencyRepository agencyRepository;
    private final MerchantRepository merchantRepository;

    @Transactional
    public AgencyResponse createAgency(AuthUser actor, AgencyCreateRequest request) {
        assertAdmin(actor);
        if (agencyRepository.existsByCode(request.code())) {
            throw BusinessException.conflict("DUPLICATE_RESOURCE", "Agency code already exists");
        }
        return AgencyResponse.from(agencyRepository.save(Agency.create(request.code(), request.name())));
    }

    @Transactional(readOnly = true)
    public List<AgencyResponse> listAgencies(AuthUser actor, boolean activeOnly) {
        assertAdmin(actor);
        return agencyRepository.findAll().stream()
                .filter(a -> !activeOnly || a.isActive())
                .map(AgencyResponse::from)
                .toList();
    }

    @Transactional
    public AgencyResponse updateAgency(AuthUser actor, Long id, AgencyUpdateRequest request) {
        assertAdmin(actor);
        Agency agency = agencyRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Agency not found"));
        if (request.name() != null && !request.name().isBlank()) {
            agency.rename(request.name());
        }
        if (request.isActive() != null) {
            agency.changeActive(request.isActive());
        }
        return AgencyResponse.from(agency);
    }

    @Transactional
    public MerchantResponse createMerchant(AuthUser actor, MerchantCreateRequest request) {
        Long agencyId = resolveAgencyIdForWrite(actor, request.agencyId());
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> BusinessException.notFound("Agency not found"));
        if (!agency.isActive()) {
            throw BusinessException.invalidState("Agency is inactive");
        }
        if (merchantRepository.existsByAgencyIdAndCode(agencyId, request.code())) {
            throw BusinessException.conflict("DUPLICATE_RESOURCE", "Merchant code already exists in agency");
        }
        return MerchantResponse.from(merchantRepository.save(
                Merchant.create(agency, request.code(), request.name())
        ));
    }

    @Transactional(readOnly = true)
    public List<MerchantResponse> listMerchants(AuthUser actor) {
        if (actor.role() == UserRole.MERCHANT) {
            throw BusinessException.forbidden("Merchants cannot list organization directory");
        }
        if (actor.role() == UserRole.AGENCY) {
            if (actor.agencyId() == null) {
                throw BusinessException.forbidden("Agency scope missing");
            }
            return merchantRepository.findByAgencyId(actor.agencyId()).stream()
                    .map(MerchantResponse::from)
                    .toList();
        }
        return merchantRepository.findAll().stream().map(MerchantResponse::from).toList();
    }

    @Transactional
    public MerchantResponse updateMerchant(AuthUser actor, Long id, MerchantUpdateRequest request) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Merchant not found"));
        assertCanManageMerchant(actor, merchant);
        if (request.name() != null && !request.name().isBlank()) {
            merchant.rename(request.name());
        }
        if (request.isActive() != null) {
            merchant.changeActive(request.isActive());
        }
        return MerchantResponse.from(merchant);
    }

    private Long resolveAgencyIdForWrite(AuthUser actor, Long requestedAgencyId) {
        if (actor.role() == UserRole.ADMIN) {
            if (requestedAgencyId == null) {
                throw BusinessException.badRequest("agencyId is required");
            }
            return requestedAgencyId;
        }
        if (actor.role() == UserRole.AGENCY) {
            if (actor.agencyId() == null) {
                throw BusinessException.forbidden("Agency scope missing");
            }
            if (requestedAgencyId != null && !requestedAgencyId.equals(actor.agencyId())) {
                throw BusinessException.forbidden("Cannot create merchant for another agency");
            }
            return actor.agencyId();
        }
        throw BusinessException.forbidden("Only ADMIN/AGENCY can create merchants");
    }

    private void assertCanManageMerchant(AuthUser actor, Merchant merchant) {
        if (actor.role() == UserRole.ADMIN) {
            return;
        }
        if (actor.role() == UserRole.AGENCY
                && actor.agencyId() != null
                && actor.agencyId().equals(merchant.getAgency().getId())) {
            return;
        }
        throw BusinessException.forbidden("Cannot manage this merchant");
    }

    private void assertAdmin(AuthUser actor) {
        if (actor.role() != UserRole.ADMIN) {
            throw BusinessException.forbidden("Only ADMIN can manage agencies");
        }
    }
}
