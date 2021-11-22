package se.jocke.nb.http.bp.core.service;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ConnectHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.StoredResponseHandler;
import io.undertow.server.handlers.proxy.ProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import java.util.function.Consumer;
import org.openide.util.Lookup;
import se.jocke.nb.http.bp.core.HttpExchange;
import se.jocke.nb.http.bp.core.decompress.DecompressService;

public class UndertowService {

    private final Undertow undertow;

    private static final int PROXY_HANDLER_MAX_REQUEST_TIME = -1;

    public UndertowService(int port, String host, Consumer<HttpExchange> listener) {
        undertow = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(createHandlerChain(listener))
                .setServerOption(UndertowOptions.DECODE_URL, true)
                .setServerOption(UndertowOptions.URL_CHARSET, "UTF-8")
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .build();
    }

    public void start() {
        undertow.start();
    }

    public void stop() {
        undertow.stop();
    }

    private HttpHandler createHandlerChain(Consumer<HttpExchange> listener) {
        OutboundProxyClient proxy = new OutboundProxyClient().setConnectionsPerThread(20);
        return new ConnectHandler(
                new UriTransformingHandler(
                        new StoredRequestHandler(
                                new StoredResponseHandler(
                                        new HttpExchangeProducerHandler(
                                                new HostHandler(proxyHandler(proxy), proxy), listener, Lookup.getDefault().lookup(DecompressService.class))
                                )
                        )
                )
        );
    }

    public static ProxyHandler proxyHandler(ProxyClient proxyClient) {
        return ProxyHandler
                .builder()
                .setMaxRequestTime(PROXY_HANDLER_MAX_REQUEST_TIME)
                .setRewriteHostHeader(true)
                .setNext(ResponseCodeHandler.HANDLE_404)
                .setProxyClient(proxyClient)
                .build();
    }
}
