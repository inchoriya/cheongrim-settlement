package com.settlehub.settlement.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fixture: docs/settlement-rules.md
 */
class SettlementCalculatorTest {

    private static final int PLATFORM_BPS = 500;
    private static final int AGENCY_BPS = 1000;
    private static final long RIDER_FEE = 3000L;

    @Nested
    @DisplayName("Example A — 기본")
    class ExampleA {

        @Test
        void calculatesBasicBreakdown() {
            SettlementBreakdown result = SettlementCalculator.calculate(
                    new SettlementCalculateCommand(20_000, 0, PLATFORM_BPS, AGENCY_BPS, RIDER_FEE)
            );

            assertThat(result.platformFeeAmount()).isEqualTo(1_000);
            assertThat(result.agencyFeeAmount()).isEqualTo(2_000);
            assertThat(result.riderFeeAmount()).isEqualTo(3_000);
            assertThat(result.merchantSettlementAmount()).isEqualTo(14_000);
            assertThat(result.tipToRiderAmount()).isEqualTo(0);
            assertThat(result.agencySettlementAmount()).isEqualTo(2_000);
            assertThat(result.hasAnomaly()).isFalse();
            assertChecksum(result);
        }
    }

    @Nested
    @DisplayName("Example B — 배달팁 포함")
    class ExampleB {

        @Test
        void tipIsPassThroughAndNotInFeeBase() {
            SettlementBreakdown result = SettlementCalculator.calculate(
                    new SettlementCalculateCommand(20_000, 2_000, PLATFORM_BPS, AGENCY_BPS, RIDER_FEE)
            );

            assertThat(result.platformFeeAmount()).isEqualTo(1_000);
            assertThat(result.agencyFeeAmount()).isEqualTo(2_000);
            assertThat(result.riderFeeAmount()).isEqualTo(3_000);
            assertThat(result.merchantSettlementAmount()).isEqualTo(14_000);
            assertThat(result.tipToRiderAmount()).isEqualTo(2_000);
            assertThat(result.riderTotalAmount()).isEqualTo(5_000);
            assertChecksum(result);
        }
    }

    @Nested
    @DisplayName("Example C — 버림 확인")
    class ExampleC {

        @Test
        void floorsFractionalFee() {
            SettlementBreakdown result = SettlementCalculator.calculate(
                    new SettlementCalculateCommand(10_001, 0, PLATFORM_BPS, AGENCY_BPS, RIDER_FEE)
            );

            assertThat(result.platformFeeAmount()).isEqualTo(500);
            assertThat(result.agencyFeeAmount()).isEqualTo(1_000);
            assertThat(result.riderFeeAmount()).isEqualTo(3_000);
            assertThat(result.merchantSettlementAmount()).isEqualTo(5_501);
            assertChecksum(result);
        }
    }

    @Nested
    @DisplayName("Example D — 음수 가맹점 정산")
    class ExampleD {

        @Test
        void normalSmallOrderStillPositive() {
            SettlementBreakdown result = SettlementCalculator.calculate(
                    new SettlementCalculateCommand(5_000, 0, PLATFORM_BPS, AGENCY_BPS, RIDER_FEE)
            );

            assertThat(result.merchantSettlementAmount()).isEqualTo(1_250);
            assertThat(result.hasAnomaly()).isFalse();
        }

        @Test
        void flagsNegativeMerchantAmount() {
            SettlementBreakdown result = SettlementCalculator.calculate(
                    new SettlementCalculateCommand(5_000, 0, PLATFORM_BPS, AGENCY_BPS, 5_000)
            );

            assertThat(result.platformFeeAmount()).isEqualTo(250);
            assertThat(result.agencyFeeAmount()).isEqualTo(500);
            assertThat(result.riderFeeAmount()).isEqualTo(5_000);
            assertThat(result.merchantSettlementAmount()).isEqualTo(-750);
            assertThat(result.anomalyFlag()).contains(AnomalyFlag.NEGATIVE_MERCHANT_AMOUNT);
            assertChecksum(result);
        }
    }

    @Nested
    @DisplayName("Example F — 대행사 정책 오버라이드 수치")
    class ExampleF {

        @Test
        void usesAgencyOverrideRates() {
            SettlementBreakdown result = SettlementCalculator.calculate(
                    new SettlementCalculateCommand(20_000, 0, 300, 1_200, 2_500)
            );

            assertThat(result.platformFeeAmount()).isEqualTo(600);
            assertThat(result.agencyFeeAmount()).isEqualTo(2_400);
            assertThat(result.riderFeeAmount()).isEqualTo(2_500);
            assertThat(result.merchantSettlementAmount()).isEqualTo(14_500);
            assertChecksum(result);
        }
    }

    @Nested
    @DisplayName("Example G — 주간 집계 합산")
    class ExampleG {

        @Test
        void aggregatesTwoOrders() {
            SettlementBreakdown o1 = SettlementCalculator.calculate(
                    new SettlementCalculateCommand(20_000, 0, PLATFORM_BPS, AGENCY_BPS, RIDER_FEE)
            );
            SettlementBreakdown o2 = SettlementCalculator.calculate(
                    new SettlementCalculateCommand(10_001, 0, PLATFORM_BPS, AGENCY_BPS, RIDER_FEE)
            );

            assertThat(o1.merchantSettlementAmount() + o2.merchantSettlementAmount()).isEqualTo(19_501);
            assertThat(o1.orderAmount() + o2.orderAmount()).isEqualTo(30_001);
            assertThat(o1.platformFeeAmount() + o2.platformFeeAmount()).isEqualTo(1_500);
            assertThat(o1.agencyFeeAmount() + o2.agencyFeeAmount()).isEqualTo(3_000);
            assertThat(o1.riderFeeAmount() + o2.riderFeeAmount()).isEqualTo(6_000);
        }
    }

    @Nested
    @DisplayName("입력 검증")
    class Validation {

        @Test
        void rejectsNegativeOrderAmount() {
            assertThatThrownBy(() -> SettlementCalculator.calculate(
                    new SettlementCalculateCommand(-1, 0, PLATFORM_BPS, AGENCY_BPS, RIDER_FEE)
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("orderAmount");
        }

        @Test
        void rejectsInvalidBps() {
            assertThatThrownBy(() -> SettlementCalculator.calculate(
                    new SettlementCalculateCommand(10_000, 0, 10_001, AGENCY_BPS, RIDER_FEE)
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("platformFeeBps");
        }

        @Test
        void flagsZeroOrderAmount() {
            SettlementBreakdown result = SettlementCalculator.calculate(
                    new SettlementCalculateCommand(0, 0, PLATFORM_BPS, AGENCY_BPS, 0)
            );

            assertThat(result.anomalyFlag()).contains(AnomalyFlag.ZERO_ORDER_AMOUNT);
        }
    }

    private static void assertChecksum(SettlementBreakdown result) {
        long sum = result.merchantSettlementAmount()
                + result.platformFeeAmount()
                + result.agencyFeeAmount()
                + result.riderFeeAmount();
        assertThat(sum).isEqualTo(result.orderAmount());
    }
}
