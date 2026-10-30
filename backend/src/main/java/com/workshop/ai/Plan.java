package com.workshop.ai;

public enum Plan {
    FREE(10, 50_000), PRO(60, 500_000), ENTERPRISE(Integer.MAX_VALUE, Long.MAX_VALUE);

    public final int requestsPerMinute;
    public final long monthlyTokenLimit;

    Plan(int requestsPerMinute, long monthlyTokenLimit) {
        this.requestsPerMinute = requestsPerMinute;
        this.monthlyTokenLimit = monthlyTokenLimit;
    }
}
