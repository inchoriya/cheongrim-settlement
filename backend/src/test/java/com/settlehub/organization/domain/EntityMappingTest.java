package com.settlehub.organization.domain;

import com.settlehub.order.domain.DeliveryOrder;
import com.settlehub.order.domain.DeliveryOrderRepository;
import com.settlehub.policy.domain.FeePolicy;
import com.settlehub.policy.domain.FeePolicyRepository;
import com.settlehub.settlement.domain.Settlement;
import com.settlehub.settlement.domain.SettlementCalculateCommand;
import com.settlehub.settlement.domain.SettlementCalculator;
import com.settlehub.settlement.domain.SettlementLine;
import com.settlehub.settlement.domain.SettlementRepository;
import com.settlehub.settlement.domain.SettlementStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import com.settlehub.config.JpaConfig;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import(JpaConfig.class)
class EntityMappingTest {

    @Autowired
    private AgencyRepository agencyRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private FeePolicyRepository feePolicyRepository;
    @Autowired
    private DeliveryOrderRepository deliveryOrderRepository;
    @Autowired
    private SettlementRepository settlementRepository;

    @Test
    @DisplayName("조직·정책·주문·정산 엔티티가 저장되고 관계가 유지된다")
    void persistsCoreGraph() {
        Agency agency = agencyRepository.save(Agency.create("AG-SEOUL-01", "서울배달"));
        Merchant merchant = merchantRepository.save(Merchant.create(agency, "M-001", "김밥천국"));
        UserAccount admin = userAccountRepository.save(
                UserAccount.admin("admin@cheongrim.local", "hash", "관리자")
        );
        userAccountRepository.save(
                UserAccount.agencyUser("agency@seoul.local", "hash", "대행담당", agency)
        );
        userAccountRepository.save(
                UserAccount.merchantUser("merchant@kimbap.local", "hash", "가맹담당", merchant)
        );

        feePolicyRepository.save(FeePolicy.builder()
                .name("글로벌 기본")
                .platformFeeBps(500)
                .agencyFeeBps(1000)
                .riderFee(3000)
                .effectiveFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .active(true)
                .build());

        LocalDateTime orderedAt = LocalDateTime.of(2026, 8, 1, 12, 30);
        DeliveryOrder order = deliveryOrderRepository.save(
                DeliveryOrder.create(agency, merchant, "ORD-001", 20_000, 0, orderedAt)
        );

        var breakdown = SettlementCalculator.calculate(
                new SettlementCalculateCommand(20_000, 0, 500, 1000, 3000)
        );

        Settlement settlement = Settlement.builder()
                .agency(agency)
                .merchant(merchant)
                .periodStart(LocalDateTime.of(2026, 7, 28, 0, 0))
                .periodEnd(LocalDateTime.of(2026, 8, 4, 0, 0))
                .status(SettlementStatus.CALCULATED)
                .orderCount(1)
                .totalOrderAmount(breakdown.orderAmount())
                .totalPlatformFeeAmount(breakdown.platformFeeAmount())
                .totalAgencySettlementAmount(breakdown.agencySettlementAmount())
                .totalRiderFeeAmount(breakdown.riderFeeAmount())
                .totalTipAmount(breakdown.tipToRiderAmount())
                .totalMerchantSettlementAmount(breakdown.merchantSettlementAmount())
                .build();
        settlement.addLine(SettlementLine.from(order, breakdown));
        settlementRepository.save(settlement);

        order.lockForSettlement();
        deliveryOrderRepository.save(order);

        Settlement saved = settlementRepository.findById(settlement.getId()).orElseThrow();
        assertThat(saved.getLines()).hasSize(1);
        assertThat(saved.getLines().get(0).getMerchantSettlementAmount()).isEqualTo(14_000);
        assertThat(saved.getTotalMerchantSettlementAmount()).isEqualTo(14_000);
        assertThat(userAccountRepository.findByEmail("admin@cheongrim.local")).isPresent();
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(feePolicyRepository.findEffectiveGlobalPolicy(orderedAt)).isPresent();
        assertThat(deliveryOrderRepository.existsByAgencyIdAndExternalOrderId(agency.getId(), "ORD-001")).isTrue();
    }

    @Test
    @DisplayName("동일 agency + externalOrderId 중복 저장은 실패한다")
    void rejectsDuplicateExternalOrderId() {
        Agency agency = agencyRepository.save(Agency.create("AG-01", "대행"));
        Merchant merchant = merchantRepository.save(Merchant.create(agency, "M-01", "가게"));
        LocalDateTime at = LocalDateTime.of(2026, 8, 1, 10, 0);

        deliveryOrderRepository.saveAndFlush(DeliveryOrder.create(agency, merchant, "DUP-1", 10_000, 0, at));

        assertThatThrownBy(() ->
                deliveryOrderRepository.saveAndFlush(
                        DeliveryOrder.create(agency, merchant, "DUP-1", 11_000, 0, at)
                )
        ).isInstanceOf(Exception.class);
    }
}
