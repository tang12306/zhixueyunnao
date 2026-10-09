package com._1.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败计数：同一用户名 + IP 在锁定时长内连续失败达到上限后，锁定一段时间。
 * 只存在内存里，重启即清空；多实例部署时需要换成 Redis 之类的共享存储。
 */
@Component
public class LoginAttemptService {

    /** 超过这个条目数时顺手清理已过期的记录，避免被大量随机用户名撑大 */
    private static final int PRUNE_THRESHOLD = 10_000;

    private final int maxFailures;
    private final Duration lockDuration;
    private final Clock clock;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /**
     * @param firstFailure 本轮第一次失败的时间，超过锁定时长后计数重新开始
     * @param lockedUntil  锁定截止时间，未锁定时为 null
     */
    private record Attempt(int failures, Instant firstFailure, Instant lockedUntil) {
    }

    @Autowired
    public LoginAttemptService(@Value("${app.login.max-failures:5}") int maxFailures,
                               @Value("${app.login.lock-minutes:15}") long lockMinutes) {
        this(maxFailures, Duration.ofMinutes(lockMinutes), Clock.systemUTC());
    }

    LoginAttemptService(int maxFailures, Duration lockDuration, Clock clock) {
        this.maxFailures = maxFailures;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    public long lockMinutes() {
        return lockDuration.toMinutes();
    }

    public static String key(String username, String clientIp) {
        return (username == null ? "" : username.trim().toLowerCase(Locale.ROOT)) + "|" + clientIp;
    }

    public boolean isLocked(String key) {
        Attempt attempt = attempts.get(key);
        return attempt != null && attempt.lockedUntil() != null && clock.instant().isBefore(attempt.lockedUntil());
    }

    public void loginFailed(String key) {
        Instant now = clock.instant();
        if (attempts.size() > PRUNE_THRESHOLD) {
            attempts.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
        }
        attempts.compute(key, (k, previous) -> {
            if (previous == null || isExpired(previous, now)) {
                return new Attempt(1, now, maxFailures <= 1 ? now.plus(lockDuration) : null);
            }
            int failures = previous.failures() + 1;
            Instant lockedUntil = failures >= maxFailures ? now.plus(lockDuration) : null;
            return new Attempt(failures, previous.firstFailure(), lockedUntil);
        });
    }

    public void loginSucceeded(String key) {
        attempts.remove(key);
    }

    private boolean isExpired(Attempt attempt, Instant now) {
        if (attempt.lockedUntil() != null) {
            return !now.isBefore(attempt.lockedUntil());
        }
        return !now.isBefore(attempt.firstFailure().plus(lockDuration));
    }
}
