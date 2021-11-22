package se.jocke.nb.http.bp.core.service;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class StoredRequestHandler implements HttpHandler {

    private final HttpHandler next;

    public StoredRequestHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.addRequestWrapper(new RequestConduitWrapper(exchange));
        next.handleRequest(exchange);
    }
}
