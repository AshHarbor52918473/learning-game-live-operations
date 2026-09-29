package learningops;

import learningops.config.InfraiSettings;
import learningops.game.GameOperationsService;
import learningops.game.GameOperationsService.LiveEvent;
import learningops.game.GameOperationsService.PlayerAsset;
import learningops.infrai.InfraiGateway;
import learningops.infrai.Json;

import java.time.Instant;

public final class GameDashboardExample {
    private GameDashboardExample() {}

    public static void main(String[] args) {
        InfraiGateway infrai = new InfraiGateway(InfraiSettings.fromEnvironment());
        infrai.createChannel("learning-game-operations:channel:v1");

        GameOperationsService operations = new GameOperationsService(infrai);
        var snapshot = operations.record(
                new PlayerAsset("asset-event-2048", "level_map", 2, false),
                new LiveEvent(184, 37, "fractions-course", Instant.now()));

        String dashboardTokenEnvelope = infrai.issueDashboardToken("teacher-dashboard-demo");
        System.out.println("Published snapshot: " + Json.write(snapshot.asMap()));
        System.out.println("Dashboard token envelope: " + dashboardTokenEnvelope);
    }
}
