package com.settlehub.payout.gateway;

/**
 * 지급 PG 포트. Mock → Toss 등으로 교체 가능.
 */
public interface PayoutGateway {

    PayoutResult requestPayout(PayoutRequest request);
}
