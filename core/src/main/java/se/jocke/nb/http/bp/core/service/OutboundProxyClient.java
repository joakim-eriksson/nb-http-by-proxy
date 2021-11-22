package se.jocke.nb.http.bp.core.service;

import io.undertow.UndertowLogger;
import io.undertow.client.ClientConnection;
import io.undertow.client.UndertowClient;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.proxy.ConnectionPoolErrorHandler;
import io.undertow.server.handlers.proxy.ConnectionPoolManager;
import io.undertow.server.handlers.proxy.ExclusivityChecker;
import io.undertow.server.handlers.proxy.ProxyCallback;
import io.undertow.server.handlers.proxy.ProxyClient;
import io.undertow.server.handlers.proxy.ProxyConnection;
import io.undertow.server.handlers.proxy.ProxyConnectionPool;
import io.undertow.server.handlers.proxy.RouteIteratorFactory;
import io.undertow.server.handlers.proxy.RouteIteratorFactory.ParsingCompatibility;
import io.undertow.server.handlers.proxy.RouteParsingStrategy;
import io.undertow.util.AttachmentKey;
import io.undertow.util.AttachmentList;
import io.undertow.util.CopyOnWriteMap;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.xnio.IoUtils.safeClose;
import org.xnio.OptionMap;
import org.xnio.ssl.XnioSsl;

/**
 * Copy of class in undertow.
 * 
 * Initial implementation of a load balancing proxy client. This initial
 * implementation is rather simplistic, and will likely change.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class OutboundProxyClient implements ProxyClient {

    /**
     * The attachment key that is used to attach the proxy connection to the
     * exchange.
     * <p>
     * This cannot be static as otherwise a connection from a different client
     * could be re-used.
     */
    private final AttachmentKey<ExclusiveConnectionHolder> exclusiveConnectionKey = AttachmentKey.create(ExclusiveConnectionHolder.class);

    private static final AttachmentKey<AttachmentList<Host>> ATTEMPTED_HOSTS = AttachmentKey.createList(Host.class);

    /**
     * Time in seconds between retries for problem servers
     */
    private volatile int problemServerRetry = 10;

    private final Set<String> sessionCookieNames = new CopyOnWriteArraySet<>();

    /**
     * The number of connections to create per thread
     */
    private volatile int connectionsPerThread = 10;
    private volatile int maxQueueSize = 0;
    private volatile int softMaxConnectionsPerThread = 5;
    private volatile int ttl = -1;

    /**
     * The hosts list.
     */
    private volatile Host[] hosts = {};

    private final HostSelector hostSelector;
    private final UndertowClient client;

    private final Map<String, Host> routes = new CopyOnWriteMap<>();
    private RouteIteratorFactory routeIteratorFactory = new RouteIteratorFactory(RouteParsingStrategy.SINGLE, ParsingCompatibility.MOD_JK);

    private final ExclusivityChecker exclusivityChecker;

    private static final ProxyClient.ProxyTarget PROXY_TARGET = new ProxyClient.ProxyTarget() {
    };

    @Override
    public List<ProxyClient.ProxyTarget> getAllTargets() {
        List<ProxyClient.ProxyTarget> arr = new ArrayList<>();
        return arr;
    }

    public OutboundProxyClient() {
        this(UndertowClient.getInstance());
    }

    public OutboundProxyClient(UndertowClient client) {
        this(client, null, null);
    }

    public OutboundProxyClient(ExclusivityChecker client) {
        this(UndertowClient.getInstance(), client, null);
    }

    public OutboundProxyClient(UndertowClient client, ExclusivityChecker exclusivityChecker) {
        this(client, exclusivityChecker, null);
    }

    public OutboundProxyClient(UndertowClient client, ExclusivityChecker exclusivityChecker, HostSelector hostSelector) {
        this.client = client;
        this.exclusivityChecker = exclusivityChecker;
        sessionCookieNames.add("JSESSIONID");
        if (hostSelector == null) {
            this.hostSelector = new RoundRobinHostSelector();
        } else {
            this.hostSelector = hostSelector;
        }
    }

    public OutboundProxyClient addSessionCookieName(final String sessionCookieName) {
        sessionCookieNames.add(sessionCookieName);
        return this;
    }

    public OutboundProxyClient removeSessionCookieName(final String sessionCookieName) {
        sessionCookieNames.remove(sessionCookieName);
        return this;
    }

    public OutboundProxyClient setProblemServerRetry(int problemServerRetry) {
        this.problemServerRetry = problemServerRetry;
        return this;
    }

    public int getProblemServerRetry() {
        return problemServerRetry;
    }

    public int getConnectionsPerThread() {
        return connectionsPerThread;
    }

    public OutboundProxyClient setConnectionsPerThread(int connectionsPerThread) {
        this.connectionsPerThread = connectionsPerThread;
        return this;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public OutboundProxyClient setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return this;
    }

    public OutboundProxyClient setTtl(int ttl) {
        this.ttl = ttl;
        return this;
    }

    public OutboundProxyClient setSoftMaxConnectionsPerThread(int softMaxConnectionsPerThread) {
        this.softMaxConnectionsPerThread = softMaxConnectionsPerThread;
        return this;
    }

    public OutboundProxyClient setRouteParsingStrategy(RouteParsingStrategy routeParsingStrategy) {
        this.routeIteratorFactory = new RouteIteratorFactory(routeParsingStrategy, ParsingCompatibility.MOD_JK, null);
        return this;
    }

    /**
     * Configures ranked route delimiter, enabling ranked routing parsing
     * strategy.
     */
    public OutboundProxyClient setRankedRoutingDelimiter(String rankedRoutingDelimiter) {
        this.routeIteratorFactory = new RouteIteratorFactory(RouteParsingStrategy.RANKED, ParsingCompatibility.MOD_JK, rankedRoutingDelimiter);
        return this;
    }

    public synchronized OutboundProxyClient addHost(final URI host) {
        return addHost(host, null, null);
    }

    public synchronized OutboundProxyClient addHost(final URI host, XnioSsl ssl) {
        return addHost(host, null, ssl);
    }

    public synchronized OutboundProxyClient addHost(final URI host, String jvmRoute) {
        return addHost(host, jvmRoute, null);
    }

    public synchronized OutboundProxyClient addHost(final URI host, String jvmRoute, XnioSsl ssl) {

        Host h = new Host(jvmRoute, null, host, ssl, OptionMap.EMPTY);
        Host[] existing = hosts;
        Host[] newHosts = new Host[existing.length + 1];
        System.arraycopy(existing, 0, newHosts, 0, existing.length);
        newHosts[existing.length] = h;
        this.hosts = newHosts;
        if (jvmRoute != null) {
            this.routes.put(jvmRoute, h);
        }
        return this;
    }

    public synchronized OutboundProxyClient addHost(final URI host, String jvmRoute, XnioSsl ssl, OptionMap options) {
        return addHost(null, host, jvmRoute, ssl, options);
    }

    public synchronized OutboundProxyClient addHost(final InetSocketAddress bindAddress, final URI host, String jvmRoute, XnioSsl ssl, OptionMap options) {
        Host h = new Host(jvmRoute, bindAddress, host, ssl, options);
        Host[] existing = hosts;
        Host[] newHosts = new Host[existing.length + 1];
        System.arraycopy(existing, 0, newHosts, 0, existing.length);
        newHosts[existing.length] = h;
        this.hosts = newHosts;
        if (jvmRoute != null) {
            this.routes.put(jvmRoute, h);
        }
        return this;
    }

    public synchronized OutboundProxyClient removeHost(final URI uri) {
        int found = -1;
        Host[] existing = hosts;
        Host removedHost = null;
        for (int i = 0; i < existing.length; ++i) {
            if (existing[i].uri.equals(uri)) {
                found = i;
                removedHost = existing[i];
                break;
            }
        }
        if (found == -1) {
            return this;
        }
        Host[] newHosts = new Host[existing.length - 1];
        System.arraycopy(existing, 0, newHosts, 0, found);
        System.arraycopy(existing, found + 1, newHosts, found, existing.length - found - 1);
        this.hosts = newHosts;
        removedHost.connectionPool.close();
        if (removedHost.jvmRoute != null) {
            routes.remove(removedHost.jvmRoute);
        }
        return this;
    }

    @Override
    public ProxyClient.ProxyTarget findTarget(HttpServerExchange exchange) {
        return PROXY_TARGET;
    }

    @Override
    public void getConnection(ProxyClient.ProxyTarget target, HttpServerExchange exchange, final ProxyCallback<ProxyConnection> callback, long timeout, TimeUnit timeUnit) {
        final ExclusiveConnectionHolder holder = exchange.getConnection().getAttachment(exclusiveConnectionKey);
        if (holder != null && holder.connection.getConnection().isOpen()) {
            // Something has already caused an exclusive connection to be allocated so keep using it.
            callback.completed(exchange, holder.connection);
            return;
        }

        final Host host = selectHost(exchange);
        if (host == null) {
            callback.couldNotResolveBackend(exchange);
        } else {
            exchange.addToAttachmentList(ATTEMPTED_HOSTS, host);
            if (holder != null || (exclusivityChecker != null && exclusivityChecker.isExclusivityRequired(exchange))) {
                // If we have a holder, even if the connection was closed we now exclusivity was already requested so our client
                // may be assuming it still exists.
                host.connectionPool.connect(target, exchange, new ProxyCallback<ProxyConnection>() {

                    @Override
                    public void completed(HttpServerExchange exchange, ProxyConnection result) {
                        if (holder != null) {
                            holder.connection = result;
                        } else {
                            final ExclusiveConnectionHolder newHolder = new ExclusiveConnectionHolder();
                            newHolder.connection = result;
                            ServerConnection connection = exchange.getConnection();
                            connection.putAttachment(exclusiveConnectionKey, newHolder);
                            connection.addCloseListener(new ServerConnection.CloseListener() {

                                @Override
                                public void closed(ServerConnection connection) {
                                    ClientConnection clientConnection = newHolder.connection.getConnection();
                                    if (clientConnection.isOpen()) {
                                        safeClose(clientConnection);
                                    }
                                }
                            });
                        }
                        callback.completed(exchange, result);
                    }

                    @Override
                    public void queuedRequestFailed(HttpServerExchange exchange) {
                        callback.queuedRequestFailed(exchange);
                    }

                    @Override
                    public void failed(HttpServerExchange exchange) {
                        UndertowLogger.PROXY_REQUEST_LOGGER.proxyFailedToConnectToBackend(exchange.getRequestURI(), host.uri);
                        callback.failed(exchange);
                    }

                    @Override
                    public void couldNotResolveBackend(HttpServerExchange exchange) {
                        callback.couldNotResolveBackend(exchange);
                    }
                }, timeout, timeUnit, true);
            } else {
                host.connectionPool.connect(target, exchange, callback, timeout, timeUnit, false);
            }
        }
    }

    /**
     * This is the only method changed as we selecting a host without access to hosts is not possible. 
     * 
     * @param exchange
     * @return 
     */
    protected Host selectHost(HttpServerExchange exchange) {
        try {
            URI uri = new URI(exchange.getRequestScheme(), exchange.getHostName(), null, null);
            for (Host host : hosts) {
                if (host.uri.equals(uri)) {
                    return host;
                }
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }

        return null;
    }

    protected Iterator<CharSequence> parseRoutes(HttpServerExchange exchange) {
        for (String cookieName : sessionCookieNames) {
            for (Cookie cookie : exchange.requestCookies()) {
                if (cookieName.equals(cookie.getName())) {
                    return routeIteratorFactory.iterator(cookie.getValue());
                }
            }
        }
        return routeIteratorFactory.iterator(null);
    }

    public final class Host extends ConnectionPoolErrorHandler.SimpleConnectionPoolErrorHandler implements ConnectionPoolManager {

        final ProxyConnectionPool connectionPool;
        final String jvmRoute;
        final URI uri;
        final XnioSsl ssl;

        private Host(String jvmRoute, InetSocketAddress bindAddress, URI uri, XnioSsl ssl, OptionMap options) {
            this.connectionPool = new ProxyConnectionPool(this, bindAddress, uri, ssl, client, options);
            this.jvmRoute = jvmRoute;
            this.uri = uri;
            this.ssl = ssl;
        }

        @Override
        public int getProblemServerRetry() {
            return problemServerRetry;
        }

        @Override
        public int getMaxConnections() {
            return connectionsPerThread;
        }

        @Override
        public int getMaxCachedConnections() {
            return connectionsPerThread;
        }

        @Override
        public int getSMaxConnections() {
            return softMaxConnectionsPerThread;
        }

        @Override
        public long getTtl() {
            return ttl;
        }

        @Override
        public int getMaxQueueSize() {
            return maxQueueSize;
        }

        public URI getUri() {
            return uri;
        }
    }

    private static class ExclusiveConnectionHolder {

        private ProxyConnection connection;

    }

    public interface HostSelector {

        int selectHost(Host[] availableHosts);
    }

    static class RoundRobinHostSelector implements HostSelector {

        private final AtomicInteger currentHost = new AtomicInteger(0);

        @Override
        public int selectHost(Host[] availableHosts) {
            return currentHost.incrementAndGet() % availableHosts.length;
        }
    }
}
