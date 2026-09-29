package learningops.infrai;

import learningops.config.InfraiSettings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfraiGateway {
    private static final Pattern OK = Pattern.compile("\\\"ok\\\"\\s*:\\s*(true|false)");
    private static final Pattern CODE = Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private final InfraiSettings settings;
    private final HttpClient http;

    public InfraiGateway(InfraiSettings settings) {
        this(settings, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    InfraiGateway(InfraiSettings settings, HttpClient http) {
        this.settings = settings;
        this.http = http;
    }

    public void createChannel(String idempotencyKey) {
        // Capability: infrai.realtime.channel.create
        post("/v1/realtime/channel/create", Map.of(
                "channel", settings.channel(), "type", "presence", "vendor", "tencent"), idempotencyKey);
    }

    public void batchMetrics(List<Map<String, Object>> points, String idempotencyKey) {
        // Capability: infrai.metrics.batch
        post("/v1/metrics/batch", Map.of("points", points, "idempotency_key", idempotencyKey), idempotencyKey);
    }

    public void publish(Map<String, Object> snapshot, String idempotencyKey) {
        // The exact metric snapshot crosses the handoff through infrai.realtime.publish.
        post("/v1/realtime/publish", Map.of(
                "channel", settings.channel(),
                "event", "game.operations.snapshot",
                "data", snapshot,
                "account_id", settings.accountId()), idempotencyKey);
    }

    public String issueDashboardToken(String clientId) {
        // Capability: infrai.realtime.token.issue; only this short-lived result belongs in a browser.
        return post("/v1/realtime/token/issue", Map.of(
                "client_id", clientId,
                "channels", List.of(settings.channel()),
                "capabilities", List.of("subscribe"),
                "ttl_seconds", 900), clientId + ":dashboard-token:v1");
    }

    protected String post(String path, Map<String, Object> body, String idempotencyKey) {
        String payload = Json.write(body);
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(settings.baseUrl() + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + settings.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Idempotency-Key", idempotencyKey)
                    .method("POST", HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response;
            try {
                response = http.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException exception) {
                throw new IllegalStateException("Infrai transport failed", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Infrai request interrupted", exception);
            }

            Envelope envelope = decodeEnvelope(response.body(), response.statusCode());
            if (response.statusCode() == 429 && attempt < 3) {
                sleep(retryMillis(response, attempt));
                continue;
            }
            if (!envelope.ok()) {
                throw new InfraiException(envelope.code(), response.statusCode(), envelope.raw());
            }
            if (response.statusCode() >= 500) {
                throw new IllegalStateException("Infrai transport status " + response.statusCode());
            }
            return envelope.raw();
        }
        throw new IllegalStateException("Retry budget exhausted");
    }

    private static Envelope decodeEnvelope(String raw, int status) {
        Matcher ok = OK.matcher(raw);
        if (!ok.find()) {
            throw new IllegalStateException("Invalid Infrai envelope at HTTP " + status);
        }
        boolean succeeded = Boolean.parseBoolean(ok.group(1));
        if (succeeded && (!raw.contains("\"data\"") || !raw.contains("\"error\"")
                || !raw.contains("\"metadata\""))) {
            throw new IllegalStateException("Invalid Infrai envelope at HTTP " + status);
        }
        Matcher code = CODE.matcher(raw);
        return new Envelope(succeeded, code.find() ? code.group(1) : "REQUEST_REJECTED", raw);
    }

    private static long retryMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .flatMap(value -> {
                    try { return java.util.Optional.of(Long.parseLong(value) * 1000L); }
                    catch (NumberFormatException ignored) { return java.util.Optional.empty(); }
                })
                .orElse(250L * (1L << attempt));
    }

    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", exception);
        }
    }

    private record Envelope(boolean ok, String code, String raw) {}
}
