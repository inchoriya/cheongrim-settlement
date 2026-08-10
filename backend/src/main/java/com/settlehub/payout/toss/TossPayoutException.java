package com.settlehub.payout.toss;

public class TossPayoutException extends RuntimeException {

    public TossPayoutException(String message) {
        super(message);
    }

    public TossPayoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
