package learningops.infrai;

public final class InfraiException extends RuntimeException {
    private final String code;
    private final int status;
    private final String detail;

    public InfraiException(String code, int status, String detail) {
        super(code + " (HTTP " + status + ")");
        this.code = code;
        this.status = status;
        this.detail = detail;
    }

    public String code() { return code; }
    public int status() { return status; }
    public String detail() { return detail; }
}
