package com._1.service.ai;

import com._1.core.exception.ApiException;
import com._1.service.ai.AiTaskService.Status;
import com._1.service.ai.AiTaskService.TaskView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiTaskServiceTests {

    private final MutableClock clock = new MutableClock();
    private final CountDownLatch release = new CountDownLatch(1);
    private AiTaskService service = new AiTaskService(2, 2, 2, clock);

    @AfterEach
    void tearDown() {
        release.countDown();
        service.shutdown();
    }

    @Test
    void successfulTaskReturnsResultAndReportsStages() throws Exception {
        CountDownLatch stageReported = new CountDownLatch(1);
        String id = service.submit("teacher", "exam", stage -> {
            stage.accept("等待 AI 回复");
            stageReported.countDown();
            await(release);
            return "试卷";
        });

        assertThat(stageReported.await(5, TimeUnit.SECONDS)).isTrue();
        TaskView running = service.get("teacher", id);
        assertThat(running.status()).isEqualTo(Status.RUNNING);
        assertThat(running.stage()).isEqualTo("等待 AI 回复");
        assertThat(running.kind()).isEqualTo("exam");

        clock.advance(Duration.ofSeconds(42));
        release.countDown();
        TaskView done = awaitFinished("teacher", id);
        assertThat(done.status()).isEqualTo(Status.SUCCEEDED);
        assertThat(done.result()).isEqualTo("试卷");
        assertThat(done.error()).isNull();
        assertThat(done.elapsedSeconds()).isEqualTo(42);
    }

    @Test
    void aiFailureKeepsStatusMessageAndRawReply() throws Exception {
        String id = service.submit("teacher", "questions", stage -> {
            throw AiServiceException.unparseable("AI 返回的内容无法解析", "这不是 JSON");
        });

        TaskView failed = awaitFinished("teacher", id);
        assertThat(failed.status()).isEqualTo(Status.FAILED);
        assertThat(failed.result()).isNull();
        assertThat(failed.error().status()).isEqualTo(502);
        assertThat(failed.error().message()).isEqualTo("AI 返回的内容无法解析");
        assertThat(failed.error().raw()).isEqualTo("这不是 JSON");
    }

    @Test
    void unexpectedErrorDoesNotLeakDetails() throws Exception {
        String id = service.submit("teacher", "questions", stage -> {
            throw new IllegalStateException("jdbc:mysql://secret-host");
        });

        TaskView failed = awaitFinished("teacher", id);
        assertThat(failed.error().status()).isEqualTo(500);
        assertThat(failed.error().message()).isEqualTo("AI 生成失败，请稍后重试").doesNotContain("secret");
    }

    @Test
    void otherTeachersCannotSeeTheTask() throws Exception {
        String id = service.submit("teacher", "exam", stage -> "试卷");
        awaitFinished("teacher", id);

        assertThatThrownBy(() -> service.get("someone.else", id))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.get("teacher", "no-such-task"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void limitsUnfinishedTasksPerTeacher() {
        service.submit("teacher", "exam", stage -> await(release));
        service.submit("teacher", "exam", stage -> await(release));

        assertThatThrownBy(() -> service.submit("teacher", "exam", stage -> "试卷"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        // 其他教师不受影响
        service.submit("another", "exam", stage -> "试卷");
    }

    @Test
    void rejectsWhenQueueIsFull() {
        service.shutdown();
        service = new AiTaskService(1, 1, 10, clock);
        service.submit("teacher", "exam", stage -> await(release));
        String queued = service.submit("teacher", "exam", stage -> "试卷");

        assertThat(service.get("teacher", queued).status()).isEqualTo(Status.QUEUED);
        assertThatThrownBy(() -> service.submit("teacher", "exam", stage -> "试卷"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE))
                .hasMessageContaining("排队已满");
    }

    @Test
    void finishedTasksExpire() throws Exception {
        String id = service.submit("teacher", "exam", stage -> "试卷");
        awaitFinished("teacher", id);

        clock.advance(AiTaskService.RETENTION.plusMinutes(1));

        assertThatThrownBy(() -> service.get("teacher", id)).hasMessageContaining("已过期");
    }

    private TaskView awaitFinished(String owner, String id) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            TaskView view = service.get(owner, id);
            if (view.status() == Status.SUCCEEDED || view.status() == Status.FAILED) {
                return view;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("任务 5 秒内没有结束");
    }

    private static Object await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "试卷";
    }

    private static final class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
