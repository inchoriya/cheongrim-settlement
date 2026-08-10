package com.settlehub.policy;

import com.settlehub.policy.domain.FeePolicy;
import com.settlehub.policy.domain.FeePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PolicyResolver {

    private final FeePolicyRepository feePolicyRepository;

    /**
     * 대행사 정책 우선, 없으면 글로벌 기본 정책.
     */
    public Optional<FeePolicy> resolve(Long agencyId, LocalDateTime orderedAt) {
        Optional<FeePolicy> agencyPolicy = feePolicyRepository.findEffectiveAgencyPolicy(agencyId, orderedAt);
        if (agencyPolicy.isPresent()) {
            return agencyPolicy;
        }
        return feePolicyRepository.findEffectiveGlobalPolicy(orderedAt);
    }
}
