package se.jocke.nb.http.bp.core.service;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HostHandler implements HttpHandler {

    private final HttpHandler next;

    private final Set<URI> targets = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final OutboundProxyClient proxyClient;

    public HostHandler(HttpHandler next, OutboundProxyClient proxyClient) {
        this.next = next;
        this.proxyClient = proxyClient;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        URI uri = new URI(exchange.getRequestScheme(), exchange.getHostName(), null, null);
        if (targets.add(uri)) {
            proxyClient.addHost(uri);
        }
        next.handleRequest(exchange);
    }
}
