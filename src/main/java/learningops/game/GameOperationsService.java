package learningops.game;

import learningops.infrai.InfraiGateway;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class GameOperationsService {
    private final InfraiGateway infrai;

    public GameOperationsService(InfraiGateway infrai) {
        this.infrai = infrai;
    }

    public DashboardSnapshot record(PlayerAsset asset, LiveEvent liveEvent) {
        int reviewDepth = asset.reportCount() > 0 || asset.educatorFlagged() ? 1 : 0;
        String moderationState = reviewDepth == 1 ? "queued" : "clear";
        DashboardSnapshot snapshot = new DashboardSnapshot(
                asset.eventId(), asset.assetKind(), liveEvent.activePlayers(),
                liveEvent.lessonCompletions(), reviewDepth, moderationState, liveEvent.observedAt());

        Map<String, String> tags = Map.of(
                "asset_kind", asset.assetKind(),
                "moderation_state", moderationState,
                "course_mode", liveEvent.courseMode());
        List<Map<String, Object>> points = List.of(
                point("learning_game.active_players", liveEvent.activePlayers(), "gauge", tags, liveEvent.observedAt()),
                point("learning_game.lesson_completions", liveEvent.lessonCompletions(), "counter", tags, liveEvent.observedAt()),
                point("learning_game.moderation_queue", reviewDepth, "gauge", tags, liveEvent.observedAt()));

        String operation = asset.eventId() + ":operations:v1";
        infrai.batchMetrics(points, operation + ":metrics");
        infrai.publish(snapshot.asMap(), operation + ":realtime");
        return snapshot;
    }

    private static Map<String, Object> point(
            String name, int value, String type, Map<String, String> tags, Instant timestamp) {
        return Map.of("name", name, "value", value, "type", type,
                "tags", tags, "timestamp", timestamp.toString());
    }

    public record PlayerAsset(String eventId, String assetKind, int reportCount, boolean educatorFlagged) {}
    public record LiveEvent(int activePlayers, int lessonCompletions, String courseMode, Instant observedAt) {}

    public record DashboardSnapshot(
            String eventId, String assetKind, int activePlayers, int lessonCompletions,
            int moderationQueueDepth, String moderationState, Instant observedAt) {
        public Map<String, Object> asMap() {
            return Map.of(
                    "event_id", eventId,
                    "asset_kind", assetKind,
                    "active_players", activePlayers,
                    "lesson_completions", lessonCompletions,
                    "moderation_queue_depth", moderationQueueDepth,
                    "moderation_state", moderationState,
                    "observed_at", observedAt.toString());
        }
    }
}
