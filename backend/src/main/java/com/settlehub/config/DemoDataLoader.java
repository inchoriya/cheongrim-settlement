package com.settlehub.config;

import com.settlehub.order.domain.DeliveryOrder;
import com.settlehub.order.domain.DeliveryOrderRepository;
import com.settlehub.order.domain.OrderStatus;
import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.AgencyRepository;
import com.settlehub.organization.domain.Merchant;
import com.settlehub.organization.domain.MerchantRepository;
import com.settlehub.organization.domain.UserAccount;
import com.settlehub.organization.domain.UserAccountRepository;
import com.settlehub.policy.domain.FeePolicy;
import com.settlehub.policy.domain.FeePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "settlehub.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataLoader implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "Demo1234!";
    public static final String ADMIN_EMAIL = "admin@cheongrim.local";
    private static final String LEGACY_ADMIN_EMAIL = "admin@settlehub.local";
    private static final String SEED_ORDER_MARKER = "ORD-SEED-001";

    private final UserAccountRepository userAccountRepository;
    private final AgencyRepository agencyRepository;
    private final MerchantRepository merchantRepository;
    private final FeePolicyRepository feePolicyRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrateLegacyAdminEmail();

        if (!userAccountRepository.existsByEmail(ADMIN_EMAIL)) {
            seedOrganizationsAndUsers();
        }

        ensureDemoOrders();
    }

    private void seedOrganizationsAndUsers() {
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        Agency agency = agencyRepository.save(Agency.create("AG-SEOUL-01", "서울배달"));
        Merchant merchant1 = merchantRepository.save(Merchant.createWithPayoutAccount(
                agency, "M-001", "김밥천국", "088", "110123456789", "김밥천국"));
        merchantRepository.save(Merchant.createWithPayoutAccount(
                agency, "M-002", "치킨하우스", "004", "123456789012", "치킨하우스"));

        userAccountRepository.save(UserAccount.admin(ADMIN_EMAIL, hash, "관리자"));
        userAccountRepository.save(UserAccount.agencyUser("agency@seoul.local", hash, "서울대행 담당", agency));
        userAccountRepository.save(UserAccount.merchantUser("merchant@kimbap.local", hash, "김밥천국 담당", merchant1));

        feePolicyRepository.save(FeePolicy.builder()
                .name("글로벌 기본")
                .platformFeeBps(500)
                .agencyFeeBps(1000)
                .riderFee(3000)
                .effectiveFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .active(true)
                .build());

        log.info("Demo org/users/policy created. admin={}", ADMIN_EMAIL);
    }

    private void ensureDemoOrders() {
        Agency agency = agencyRepository.findByCode("AG-SEOUL-01").orElse(null);
        if (agency == null) {
            log.warn("Skip demo orders: agency AG-SEOUL-01 not found");
            return;
        }
        if (deliveryOrderRepository.existsByAgencyIdAndExternalOrderId(agency.getId(), SEED_ORDER_MARKER)) {
            return;
        }

        Merchant m1 = merchantRepository.findByAgencyIdAndCode(agency.getId(), "M-001")
                .orElseThrow(() -> new IllegalStateException("Merchant M-001 missing"));
        Merchant m2 = merchantRepository.findByAgencyIdAndCode(agency.getId(), "M-002")
                .orElseThrow(() -> new IllegalStateException("Merchant M-002 missing"));

        List<DeliveryOrder> orders = List.of(
                // 면접 검산 Example G용 (합 가맹점 정산 19,501)
                order(agency, m1, SEED_ORDER_MARKER, 20_000, 0, LocalDateTime.of(2026, 8, 2, 12, 0)),
                order(agency, m1, "ORD-SEED-002", 10_001, 0, LocalDateTime.of(2026, 8, 3, 13, 0)),
                // 추가 더미
                order(agency, m1, "ORD-SEED-003", 35_000, 2_000, LocalDateTime.of(2026, 8, 4, 11, 20)),
                order(agency, m1, "ORD-SEED-004", 15_500, 0, LocalDateTime.of(2026, 8, 4, 18, 45)),
                order(agency, m2, "ORD-SEED-005", 28_000, 1_000, LocalDateTime.of(2026, 8, 5, 12, 10)),
                order(agency, m2, "ORD-SEED-006", 19_900, 0, LocalDateTime.of(2026, 8, 5, 19, 30)),
                order(agency, m2, "ORD-SEED-007", 42_000, 3_000, LocalDateTime.of(2026, 8, 6, 13, 15)),
                order(agency, m1, "ORD-SEED-008", 9_900, 500, LocalDateTime.of(2026, 8, 6, 20, 5)),
                // 취소 건 (배치 대상 제외)
                DeliveryOrder.create(
                        agency, m1, "ORD-SEED-009", 16_000, 0,
                        LocalDateTime.of(2026, 8, 6, 21, 0), OrderStatus.CANCELLED),
                order(agency, m1, "ORD-SEED-010", 23_400, 0, LocalDateTime.of(2026, 8, 7, 9, 40))
        );

        deliveryOrderRepository.saveAll(orders);
        log.info("Demo orders seeded: {}건 (기간 2026-08-01 ~ 2026-08-08 배치용)", orders.size());
    }

    private static DeliveryOrder order(
            Agency agency,
            Merchant merchant,
            String externalId,
            long amount,
            long tip,
            LocalDateTime orderedAt
    ) {
        return DeliveryOrder.create(agency, merchant, externalId, amount, tip, orderedAt);
    }

    private void migrateLegacyAdminEmail() {
        userAccountRepository.findByEmail(LEGACY_ADMIN_EMAIL).ifPresent(user -> {
            if (userAccountRepository.existsByEmail(ADMIN_EMAIL)) {
                log.warn("Both legacy and new admin emails exist; leaving legacy account as-is");
                return;
            }
            user.changeEmail(ADMIN_EMAIL);
            log.info("Migrated demo admin email {} -> {}", LEGACY_ADMIN_EMAIL, ADMIN_EMAIL);
        });
    }
}
