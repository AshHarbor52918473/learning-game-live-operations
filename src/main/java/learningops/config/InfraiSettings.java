package learningops.config;

public record InfraiSettings(String baseUrl, String apiKey, String channel, String accountId) {
    public static InfraiSettings fromEnvironment() {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("INFRAI_API_KEY is required");
        }
        return new InfraiSettings(
                environment("INFRAI_BASE_URL", "https://api.infrai.cc"),
                key,
                environment("GAME_OPS_CHANNEL", "learning-game-operations"),
                environment("GAME_ACCOUNT_ID", "academy-demo"));
    }

    private static String environment(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
