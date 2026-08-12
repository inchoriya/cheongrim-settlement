package com.settlehub.config;

import com.settlehub.auth.security.AuthUser;
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
import com.settlehub.settlement.api.SettlementBatchRequest;
import com.settlehub.settlement.api.SettlementBatchResponse;
import com.settlehub.settlement.api.SettlementBatchService;
import com.settlehub.settlement.domain.SettlementRepository;
import com.settlehub.settlement.domain.SettlementStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "settlehub.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataLoader implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "Demo1234!";
    public static final String ADMIN_EMAIL = "admin@cheongnim.local";
    private static final List<String> LEGACY_ADMIN_EMAILS = List.of(
            "admin@settlehub.local",
            "admin@cheongrim.local"
    );
    private static final String SEED_ORDER_MARKER = "ORD-SEED-001";
    private static final LocalDateTime SEED_PERIOD_START = LocalDateTime.of(2026, 8, 1, 0, 0);
    private static final LocalDateTime SEED_PERIOD_END = LocalDateTime.of(2026, 8, 8, 0, 0);

    private final UserAccountRepository userAccountRepository;
    private final AgencyRepository agencyRepository;
    private final MerchantRepository merchantRepository;
    private final FeePolicyRepository feePolicyRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementBatchService settlementBatchService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrateLegacyAdminEmail();

        if (!userAccountRepository.existsByEmail(ADMIN_EMAIL)) {
            seedOrganizationsAndUsers();
        }

        ensureDemoOrders();
        ensureDemoSettlements();
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

    /**
     * 시드 주문으로 정산 배치를 한 번 돌려 둔다.
     *
     * <p>정산 건이 없으면 최초 실행 직후 목록·정산서 화면이 비어 있어, 데모를 보는 쪽이
     * 배치를 직접 돌리기 전까지는 아무것도 확인할 수 없다. 실제 배치 경로를 그대로 태워
     * 화면에서 보이는 값과 계산 로직이 어긋나지 않게 한다.
     *
     * <p>이미 해당 기간에 정산이 있으면 손대지 않는다. 데모 중 확정·지급까지 진행한 뒤
     * 재시작하면 배치가 거부되는데, 그것 때문에 애플리케이션 기동이 실패하면 안 되기 때문이다.
     */
    private void ensureDemoSettlements() {
        Agency agency = agencyRepository.findByCode("AG-SEOUL-01").orElse(null);
        if (agency == null) {
            return;
        }
        boolean alreadySettled = settlementRepository.existsByAgencyIdAndPeriodStartAndPeriodEndAndStatusIn(
                agency.getId(), SEED_PERIOD_START, SEED_PERIOD_END, EnumSet.allOf(SettlementStatus.class));
        if (alreadySettled) {
            return;
        }

        UserAccount admin = userAccountRepository.findByEmail(ADMIN_EMAIL).orElse(null);
        if (admin == null) {
            log.warn("Skip demo settlements: admin account not found");
            return;
        }

        try {
            SettlementBatchResponse result = settlementBatchService.runBatch(
                    AuthUser.from(admin),
                    new SettlementBatchRequest(SEED_PERIOD_START, SEED_PERIOD_END, agency.getId())
            );
            log.info("Demo settlements seeded: {}건 (주문 {}건 처리, 기간 {} ~ {})",
                    result.createdSettlementCount(), result.processedOrderCount(),
                    SEED_PERIOD_START.toLocalDate(), SEED_PERIOD_END.toLocalDate());
        } catch (RuntimeException ex) {
            log.warn("Skip demo settlement seeding: {}", ex.getMessage());
        }
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
        for (String legacyEmail : LEGACY_ADMIN_EMAILS) {
            userAccountRepository.findByEmail(legacyEmail).ifPresent(user -> {
                if (userAccountRepository.existsByEmail(ADMIN_EMAIL)) {
                    log.warn("Both legacy and new admin emails exist; leaving {} as-is", legacyEmail);
                    return;
                }
                user.changeEmail(ADMIN_EMAIL);
                log.info("Migrated demo admin email {} -> {}", legacyEmail, ADMIN_EMAIL);
            });
        }
    }
}
