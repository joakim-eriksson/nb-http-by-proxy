package se.jocke.nb.http.bp.core.config;

public enum ProxyConfig {
    BIND_ADDRESS("bind.address"),
    PORT("port");

    private final String key;

    private ProxyConfig(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
