package com.workshop.ai;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.YearMonth;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    private String userId;
    private String plan;
    private long monthlyTokensUsed;
    private String usageMonth;

    protected Account() {}

    Account(String userId) {
        this.userId = userId;
        this.plan = Plan.FREE.name();
        this.usageMonth = YearMonth.now().toString();
    }

    public String getUserId() { return userId; }
    public Plan getPlan() { return Plan.valueOf(plan); }
    public long getMonthlyTokensUsed() { return monthlyTokensUsed; }
    public void setPlan(Plan plan) { this.plan = plan.name(); }
    public void addUsage(long tokens) { this.monthlyTokensUsed += tokens; }
    public void resetIfNewMonth() {
        String current = YearMonth.now().toString();
        if (!current.equals(usageMonth)) {
            usageMonth = current;
            monthlyTokensUsed = 0;
        }
    }
}
