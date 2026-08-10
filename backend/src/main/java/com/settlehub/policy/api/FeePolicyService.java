package com.settlehub.policy.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.exception.BusinessException;
import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.AgencyRepository;
import com.settlehub.organization.domain.UserRole;
import com.settlehub.policy.PolicyResolver;
import com.settlehub.policy.domain.FeePolicy;
import com.settlehub.policy.domain.FeePolicyRepository;
import com.settlehub.settlement.domain.SettlementBreakdown;
import com.settlehub.settlement.domain.SettlementCalculateCommand;
import com.settlehub.settlement.domain.SettlementCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeePolicyService {

    private final FeePolicyRepository feePolicyRepository;
    private final AgencyRepository agencyRepository;
    private final PolicyResolver policyResolver;

    @Transactional
    public FeePolicyResponse create(AuthUser actor, FeePolicyRequest request) {
        if (actor.role() != UserRole.ADMIN) {
            throw BusinessException.forbidden("Only ADMIN can create fee policies");
        }
        if (request.effectiveTo() != null && !request.effectiveFrom().isBefore(request.effectiveTo())) {
            throw BusinessException.badRequest("effectiveFrom must be before effectiveTo");
        }

        Agency agency = null;
        if (request.agencyId() != null) {
            agency = agencyRepository.findById(request.agencyId())
                    .orElseThrow(() -> BusinessException.notFound("Agency not found"));
        }

        FeePolicy saved = feePolicyRepository.save(FeePolicy.builder()
                .agency(agency)
                .name(request.name())
                .platformFeeBps(request.platformFeeBps())
                .agencyFeeBps(request.agencyFeeBps())
                .riderFee(request.riderFee())
                .effectiveFrom(request.effectiveFrom())
                .effectiveTo(request.effectiveTo())
                .active(true)
                .build());
        return FeePolicyResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FeePolicyResponse> list(AuthUser actor) {
        assertCanReadPolicy(actor);
        List<FeePolicy> result = new ArrayList<>();
        if (actor.role() == UserRole.ADMIN) {
            result.addAll(feePolicyRepository.findAll().stream().filter(FeePolicy::isActive).toList());
        } else {
            result.addAll(feePolicyRepository.findByAgencyIsNullAndActiveTrue());
            result.addAll(feePolicyRepository.findByAgencyIdAndActiveTrue(actor.agencyId()));
        }
        return result.stream().map(FeePolicyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FeePolicyResponse resolve(AuthUser actor, Long agencyId, LocalDateTime orderedAt) {
        assertCanReadPolicy(actor);
        Long targetAgencyId = agencyId;
        if (actor.role() == UserRole.AGENCY) {
            targetAgencyId = actor.agencyId();
        }
        if (targetAgencyId == null) {
            throw BusinessException.badRequest("agencyId is required");
        }
        if (orderedAt == null) {
            orderedAt = LocalDateTime.now();
        }
        FeePolicy policy = policyResolver.resolve(targetAgencyId, orderedAt)
                .orElseThrow(() -> BusinessException.notFound("No effective policy"));
        return FeePolicyResponse.from(policy);
    }

    @Transactional(readOnly = true)
    public SettlementBreakdown preview(
            AuthUser actor,
            long orderAmount,
            long deliveryTip,
            Long agencyId,
            LocalDateTime orderedAt
    ) {
        assertCanReadPolicy(actor);
        Long targetAgencyId = agencyId;
        if (actor.role() == UserRole.AGENCY) {
            targetAgencyId = actor.agencyId();
        }
        if (targetAgencyId == null) {
            throw BusinessException.badRequest("agencyId is required");
        }
        if (orderedAt == null) {
            orderedAt = LocalDateTime.now();
        }
        FeePolicy policy = policyResolver.resolve(targetAgencyId, orderedAt)
                .orElseThrow(() -> new BusinessException(
                        "POLICY_NOT_FOUND",
                        "No effective policy",
                        org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
                ));
        return SettlementCalculator.calculate(new SettlementCalculateCommand(
                orderAmount,
                deliveryTip,
                policy.getPlatformFeeBps(),
                policy.getAgencyFeeBps(),
                policy.getRiderFee()
        ));
    }

    private void assertCanReadPolicy(AuthUser actor) {
        if (actor.role() == UserRole.MERCHANT) {
            throw BusinessException.forbidden("Merchants cannot access fee policies");
        }
    }
}
