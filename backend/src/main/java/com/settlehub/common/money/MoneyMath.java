package com.settlehub.common.money;

/**
 * 원 단위(Long) 수수료 계산 헬퍼.
 */
public final class MoneyMath {

    private MoneyMath() {
    }

    public static long feeByBps(long amount, int bps) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        if (bps < 0 || bps > 10_000) {
            throw new IllegalArgumentException("bps must be between 0 and 10000");
        }
        return Math.floorDiv(amount * (long) bps, 10_000L);
    }
}
