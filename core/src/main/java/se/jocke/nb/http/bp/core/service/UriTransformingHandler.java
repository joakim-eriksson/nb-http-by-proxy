package se.jocke.nb.http.bp.core.service;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.QueryParameterUtils;

public class UriTransformingHandler implements HttpHandler {

    private final HttpHandler next;

    public UriTransformingHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String requestUriString = exchange.getRequestURI();
        String[] split = requestUriString.split("\\?", 2);
        if (split.length == 2) {
            exchange.setRequestURI(split[0]);
            exchange.setQueryString(split[1]);
            exchange.getQueryParameters().putAll(QueryParameterUtils.parseQueryString(split[1], "UTF-8"));
        }
        next.handleRequest(exchange);
    }
}
