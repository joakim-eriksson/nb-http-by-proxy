package se.jocke.nb.http.bp.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class HttpExchange implements Comparable<HttpExchange> {

    private final static AtomicInteger eventCounter = new AtomicInteger(0);

    public static final HttpExchange EMPTY = new HttpExchange.Builder().build();

    private final Integer eventId;

    private final Map<String, String> requestHeaders;

    private final Map<String, String> responseHeaders;

    private final Map<String, String> requestCookies;

    private final Map<String, String> responseCookies;

    private final Map<String, String> queryParameters;

    private final String method;

    private final Supplier<String> responseBody;

    private final Supplier<String> requestBody;

    private final int contentLength;

    private final String requestURI;

    private final int status;

    private final long duration;

    public HttpExchange(Builder builder) {
        this.eventId = builder.eventId;
        this.requestHeaders = builder.requestHeaders;
        this.responseHeaders = builder.responseHeaders;
        this.requestCookies = builder.requestCookies;
        this.responseCookies = builder.responseCookies;
        this.queryParameters = builder.queryParameters;
        this.method = builder.method;
        this.responseBody = builder.responseBody;
        this.requestBody = builder.requestBody;
        this.contentLength = builder.contentLength;
        this.requestURI = builder.requestURI;
        this.status = builder.status;
        this.duration = builder.getDuration();
    }

    public Map<String, String> getRequestHeaders() {
        return copyOfMap(requestHeaders);
    }

    public Map<String, String> getResponseHeaders() {
        return copyOfMap(responseHeaders);
    }

    public Map<String, String> getRequestCookies() {
        return copyOfMap(requestCookies);
    }

    public Map<String, String> getQueryParameters() {
        return copyOfMap(queryParameters);
    }

    public Map<String, String> getResponseCookies() {
        return copyOfMap(responseCookies);
    }

    private static Map<String, String> copyOfMap(Map<String, String> toCopy) {
        return new LinkedHashMap<>(toCopy);
    }

    public String getMethod() {
        return method;
    }

    public String getResponseBody() {
        return responseBody.get();
    }

    public String getRequestBody() {
        return requestBody.get();
    }

    public int getContentLength() {
        return contentLength;
    }

    public Integer getEventId() {
        return eventId;
    }

    public String getRequestURI() {
        return requestURI;
    }

    public int getStatus() {
        return status;
    }

    public long getDuration() {
        return duration;
    }

    @Override
    public int compareTo(HttpExchange event) {
        return Integer.compare(this.eventId, event.eventId);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.eventId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HttpExchange other = (HttpExchange) obj;
        return Objects.equals(this.eventId, other.eventId);
    }

    @Override
    public String toString() {
        return "HttpExchange{" + "eventId=" + eventId + ", requestURI=" + requestURI + '}';
    }

    public static final class Builder {

        private final Integer eventId;
        private Map<String, String> requestHeaders = new LinkedHashMap<>();
        private Map<String, String> responseHeaders = new LinkedHashMap<>();
        private Map<String, String> requestCookies = new LinkedHashMap<>();
        private Map<String, String> responseCookies = new LinkedHashMap<>();
        private Map<String, String> queryParameters = new LinkedHashMap<>();
        private String method;
        private Supplier<String> responseBody;
        private Supplier<String> requestBody;
        private int contentLength;
        private String requestURI;
        private int status;
        private final long start;
        private long end;

        public Builder() {
            this.eventId = eventCounter.incrementAndGet();
            this.start = System.currentTimeMillis();
        }

        public Builder requestHeaders(Map<String, String> requestHeaders) {
            this.requestHeaders = requestHeaders;
            return this;
        }

        public Builder responseHeaders(Map<String, String> responseHeaders) {
            this.responseHeaders = responseHeaders;
            return this;
        }

        public Builder requestCookies(Map<String, String> cookies) {
            this.requestCookies = cookies;
            return this;
        }

        public Builder responseCookies(Map<String, String> cookies) {
            this.responseCookies = cookies;
            return this;
        }

        public Builder queryParameters(Map<String, String> queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder responseBody(Supplier<String> responseBody) {
            this.responseBody = responseBody;
            return this;
        }

        public Builder requestBody(Supplier<String> requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        public Builder contentLength(int contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public Builder requestURI(String requestURI) {
            this.requestURI = requestURI;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder end() {
            this.end = System.currentTimeMillis();
            return this;
        }

        public long getDuration() {
            return this.end > 0 ? end - start : 0l;
        }

        public HttpExchange build() {
            return new HttpExchange(this);
        }
    }
}
