package learningops.game;

import learningops.config.InfraiSettings;
import learningops.infrai.InfraiGateway;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class GameOperationsServiceTest {
    public static void main(String[] args) {
        RecordingGateway gateway = new RecordingGateway();
        GameOperationsService service = new GameOperationsService(gateway);
        Instant observedAt = Instant.parse("2026-09-27T08:30:00Z");

        var result = service.record(
                new GameOperationsService.PlayerAsset("evt-lesson-7", "student_level", 3, false),
                new GameOperationsService.LiveEvent(42, 11, "geometry-course", observedAt));

        check(result.moderationQueueDepth() == 1, "reported asset must enter the moderation queue");
        check(result.moderationState().equals("queued"), "dashboard must name the queue transition");
        check(gateway.points.size() == 3, "one batch must contain the three operational measures");
        check(gateway.published.equals(result.asMap()), "realtime must receive the exact computed snapshot");
        check(gateway.metricKey.equals("evt-lesson-7:operations:v1:metrics"), "metric write must be idempotent");
        check(gateway.publishKey.equals("evt-lesson-7:operations:v1:realtime"), "publish must be idempotent");
        System.out.println("PASS reported player asset is queued and its snapshot is handed to realtime");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class RecordingGateway extends InfraiGateway {
        private List<Map<String, Object>> points;
        private Map<String, Object> published;
        private String metricKey;
        private String publishKey;

        private RecordingGateway() {
            super(new InfraiSettings("https://example.test", "unit-test-key", "test-channel", "test-account"));
        }

        @Override
        public void batchMetrics(List<Map<String, Object>> value, String idempotencyKey) {
            points = value;
            metricKey = idempotencyKey;
        }

        @Override
        public void publish(Map<String, Object> value, String idempotencyKey) {
            published = value;
            publishKey = idempotencyKey;
        }
    }
}
