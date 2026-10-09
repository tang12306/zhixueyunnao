package com._1.service.ai;

import com._1.core.exception.ApiException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * AI 生成任务：一次生成要几十秒到几分钟，不能占着 HTTP 请求等。
 * <p>
 * 提交后立即返回任务 id，前端轮询 {@link #get}。线程池大小固定（默认 4），排队数有上限，
 * 每个教师同时进行的任务数也有上限；任务结果只保存在内存里，结束 30 分钟后清理，重启后丢失。
 */
@Service
public class AiTaskService {

    private static final Logger log = LoggerFactory.getLogger(AiTaskService.class);
    static final Duration RETENTION = Duration.ofMinutes(30);
    static final String QUEUED_STAGE = "排队中";

    public enum Status { QUEUED, RUNNING, SUCCEEDED, FAILED }

    /** 失败原因，raw 是模型的原始输出（解析失败时才有） */
    public record TaskError(int status, String message, String raw) {
    }

    /** 返回给前端的任务快照 */
    public record TaskView(String id, String kind, Status status, String stage, long elapsedSeconds,
                           Object result, TaskError error) {
    }

    private final ThreadPoolExecutor executor;
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final int maxActivePerUser;
    private final Clock clock;

    @Autowired
    public AiTaskService(@Value("${app.ai.max-concurrent-tasks:4}") int maxConcurrent,
                         @Value("${app.ai.task-queue-capacity:20}") int queueCapacity,
                         @Value("${app.ai.max-active-tasks-per-user:2}") int maxActivePerUser) {
        this(maxConcurrent, queueCapacity, maxActivePerUser, Clock.systemUTC());
    }

    AiTaskService(int maxConcurrent, int queueCapacity, int maxActivePerUser, Clock clock) {
        AtomicInteger threadCount = new AtomicInteger();
        this.executor = new ThreadPoolExecutor(maxConcurrent, maxConcurrent, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity), runnable -> {
                    Thread thread = new Thread(runnable, "ai-task-" + threadCount.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });
        this.executor.allowCoreThreadTimeOut(true);
        this.maxActivePerUser = maxActivePerUser;
        this.clock = clock;
    }

    /**
     * 提交任务。work 收到一个回调，用来报告当前进度（显示给教师），返回值就是任务结果。
     *
     * @throws ApiException 429 该教师进行中的任务太多；503 排队已满
     */
    public synchronized String submit(String owner, String kind, Function<Consumer<String>, Object> work) {
        pruneExpired();
        long active = tasks.values().stream().filter(t -> t.owner.equals(owner) && !t.isFinished()).count();
        if (active >= maxActivePerUser) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "你已有 " + active + " 个 AI 任务在进行，请等它们完成后再提交");
        }

        Task task = new Task(UUID.randomUUID().toString(), owner, kind, clock.instant());
        tasks.put(task.id, task);
        try {
            executor.execute(() -> run(task, work));
        } catch (RejectedExecutionException e) {
            tasks.remove(task.id);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI 任务排队已满，请稍后再试");
        }
        log.info("AI 任务已提交：{} {}（用户 {}）", kind, task.id, owner);
        return task.id;
    }

    /** 只能查看自己的任务；别人的任务和已清理的任务都按不存在处理 */
    public TaskView get(String owner, String id) {
        pruneExpired();
        Task task = tasks.get(id);
        if (task == null || !task.owner.equals(owner)) {
            throw ApiException.notFound("任务不存在或已过期");
        }
        return task.view(clock.instant());
    }

    private void run(Task task, Function<Consumer<String>, Object> work) {
        task.startedAt = clock.instant();
        task.stage = "开始生成";
        task.status = Status.RUNNING;
        try {
            Object result = work.apply(stage -> task.stage = stage);
            task.finish(Status.SUCCEEDED, result, null, clock.instant());
            log.info("AI 任务完成：{}，用时 {} 秒", task.id, task.view(clock.instant()).elapsedSeconds());
        } catch (AiServiceException e) {
            log.warn("AI 任务失败：{}，{}", task.id, e.getMessage());
            task.finish(Status.FAILED, null, new TaskError(e.getStatus().value(), e.getMessage(), e.getRaw()),
                    clock.instant());
        } catch (ApiException e) {
            log.warn("AI 任务失败：{}，{}", task.id, e.getMessage());
            task.finish(Status.FAILED, null, new TaskError(e.getStatus().value(), e.getMessage(), null),
                    clock.instant());
        } catch (IllegalArgumentException e) {
            task.finish(Status.FAILED, null, new TaskError(400, e.getMessage(), null), clock.instant());
        } catch (RuntimeException e) {
            log.error("AI 任务出现未预期的错误：{}", task.id, e);
            task.finish(Status.FAILED, null, new TaskError(500, "AI 生成失败，请稍后重试", null), clock.instant());
        }
    }

    private void pruneExpired() {
        Instant cutoff = clock.instant().minus(RETENTION);
        tasks.values().removeIf(t -> t.finishedAt != null && t.finishedAt.isBefore(cutoff));
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private static final class Task {
        final String id;
        final String owner;
        final String kind;
        final Instant createdAt;
        volatile Instant startedAt;
        volatile Instant finishedAt;
        volatile Status status = Status.QUEUED;
        volatile String stage = QUEUED_STAGE;
        volatile Object result;
        volatile TaskError error;

        Task(String id, String owner, String kind, Instant createdAt) {
            this.id = id;
            this.owner = owner;
            this.kind = kind;
            this.createdAt = createdAt;
        }

        boolean isFinished() {
            return status == Status.SUCCEEDED || status == Status.FAILED;
        }

        void finish(Status finalStatus, Object result, TaskError error, Instant now) {
            this.result = result;
            this.error = error;
            this.stage = finalStatus == Status.SUCCEEDED ? "已完成" : "失败";
            this.finishedAt = now;
            // 最后写 status：轮询看到 SUCCEEDED 时 result 一定已经就绪
            this.status = finalStatus;
        }

        TaskView view(Instant now) {
            Status current = status;
            Instant end = finishedAt != null ? finishedAt : now;
            long elapsed = Duration.between(createdAt, end).toSeconds();
            return new TaskView(id, kind, current, stage, elapsed, result, error);
        }
    }
}
