package com.settlehub.policy;

import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.AgencyRepository;
import com.settlehub.policy.domain.FeePolicy;
import com.settlehub.policy.domain.FeePolicyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import com.settlehub.config.JpaConfig;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JpaConfig.class, PolicyResolver.class})
class PolicyResolverTest {

    @Autowired
    private PolicyResolver policyResolver;
    @Autowired
    private FeePolicyRepository feePolicyRepository;
    @Autowired
    private AgencyRepository agencyRepository;

    @Test
    void prefersAgencyPolicyOverGlobal() {
        Agency agency = agencyRepository.save(Agency.create("AG-X", "대행X"));
        LocalDateTime at = LocalDateTime.of(2026, 8, 1, 12, 0);

        feePolicyRepository.save(FeePolicy.builder()
                .name("global")
                .platformFeeBps(500)
                .agencyFeeBps(1000)
                .riderFee(3000)
                .effectiveFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .active(true)
                .build());

        feePolicyRepository.save(FeePolicy.builder()
                .agency(agency)
                .name("agency")
                .platformFeeBps(300)
                .agencyFeeBps(1200)
                .riderFee(2500)
                .effectiveFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .active(true)
                .build());

        FeePolicy resolved = policyResolver.resolve(agency.getId(), at).orElseThrow();
        assertThat(resolved.getPlatformFeeBps()).isEqualTo(300);
        assertThat(resolved.getAgencyFeeBps()).isEqualTo(1200);
        assertThat(resolved.getRiderFee()).isEqualTo(2500);
    }
}
