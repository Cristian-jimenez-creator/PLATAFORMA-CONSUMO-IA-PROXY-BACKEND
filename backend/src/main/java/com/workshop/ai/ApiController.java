package com.workshop.ai;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final AccountRepository accounts;
    private final UsageRepository usage;
    private final Map<String, Deque<Instant>> requestTimes = new ConcurrentHashMap<>();

    public ApiController(AccountRepository accounts, UsageRepository usage) {
        this.accounts = accounts;
        this.usage = usage;
    }

    public record GenerateRequest(String userId, String prompt) {}
    public record UpgradeRequest(String userId) {}

    private Account account(String userId) {
        if (userId == null || userId.isBlank() || userId.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid userId");
        Account account = accounts.findById(userId).orElseGet(() -> accounts.save(new Account(userId)));
        account.resetIfNewMonth();
        return accounts.save(account);
    }

    @PostMapping("/ai/generate")
    @Transactional
    public Map<String, Object> generate(@RequestBody GenerateRequest body) {
        if (body.prompt() == null || body.prompt().isBlank() || body.prompt().length() > 8000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prompt must contain 1 to 8000 characters");
        Account account = account(body.userId());
        checkRateLimit(account);
        long tokens = Math.max(1, (body.prompt().length() + 3L) / 4L);
        if (account.getPlan().monthlyTokenLimit != Long.MAX_VALUE
                && account.getMonthlyTokensUsed() + tokens > account.getPlan().monthlyTokenLimit)
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Monthly quota reached");
        String response = "Respuesta simulada: " + body.prompt().trim();
        long outputTokens = Math.max(1, (response.length() + 3L) / 4L);
        long total = tokens + outputTokens;
        if (account.getPlan().monthlyTokenLimit != Long.MAX_VALUE
                && account.getMonthlyTokensUsed() + total > account.getPlan().monthlyTokenLimit)
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Monthly quota reached");
        account.addUsage(total);
        accounts.save(account);
        usage.save(new UsageRecord(body.userId(), total));
        return Map.of("response", response, "tokensUsed", total, "plan", account.getPlan().name());
    }

    private void checkRateLimit(Account account) {
        if (account.getPlan().requestsPerMinute == Integer.MAX_VALUE) return;
        Instant now = Instant.now();
        Deque<Instant> times = requestTimes.computeIfAbsent(account.getUserId(), ignored -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(now.minusSeconds(60))) times.removeFirst();
            if (times.size() >= account.getPlan().requestsPerMinute) {
                long retry = Math.max(1, 60 - (now.getEpochSecond() - times.peekFirst().getEpochSecond()));
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit reached; retry in " + retry + " seconds");
            }
            times.addLast(now);
        }
    }

    @GetMapping("/quota")
    public Map<String, Object> quota(@RequestParam String userId) {
        Account account = account(userId);
        Plan plan = account.getPlan();
        long limit = plan.monthlyTokenLimit;
        Map<String, Object> result = new HashMap<>();
        result.put("plan", plan.name());
        result.put("tokensUsed", account.getMonthlyTokensUsed());
        result.put("monthlyLimit", limit == Long.MAX_VALUE ? null : limit);
        result.put("tokensRemaining", limit == Long.MAX_VALUE ? null : Math.max(0, limit - account.getMonthlyTokensUsed()));
        result.put("requestsPerMinute", plan.requestsPerMinute == Integer.MAX_VALUE ? null : plan.requestsPerMinute);
        return result;
    }

    @GetMapping("/quota/history")
    public Map<String, Object> history(@RequestParam String userId) {
        account(userId);
        Instant since = LocalDate.now(ZoneOffset.UTC).minusDays(6).atStartOfDay().toInstant(ZoneOffset.UTC);
        Map<LocalDate, Long> totals = new HashMap<>();
        for (int i = 6; i >= 0; i--) totals.put(LocalDate.now(ZoneOffset.UTC).minusDays(i), 0L);
        for (UsageRecord record : usage.findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(userId, since)) {
            LocalDate day = record.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            totals.computeIfPresent(day, (key, value) -> value + record.getTokens());
        }
        var days = new java.util.ArrayList<Map<String, Object>>();
        totals.forEach((day, tokens) -> days.add(Map.of("date", day.toString(), "tokens", tokens)));
        days.sort(java.util.Comparator.comparing(item -> item.get("date").toString()));
        return Map.of("days", days);
    }

    @PostMapping("/quota/upgrade")
    @Transactional
    public Map<String, Object> upgrade(@RequestBody UpgradeRequest body) {
        Account account = account(body.userId());
        if (account.getPlan() == Plan.FREE) account.setPlan(Plan.PRO);
        accounts.save(account);
        return Map.of("plan", account.getPlan().name(), "message", "Plan updated");
    }
}
