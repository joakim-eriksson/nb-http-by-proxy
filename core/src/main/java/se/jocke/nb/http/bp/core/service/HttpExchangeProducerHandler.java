package se.jocke.nb.http.bp.core.service;

import io.undertow.attribute.StoredResponse;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import java.util.AbstractMap.SimpleEntry;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import java.util.stream.StreamSupport;
import se.jocke.nb.http.bp.core.HttpExchange;
import se.jocke.nb.http.bp.core.decompress.DecompressService;

public class HttpExchangeProducerHandler implements HttpHandler {

    private final HttpHandler next;

    private final Consumer<HttpExchange> eventConsumer;
    
    private final DecompressService decompressService;

    public HttpExchangeProducerHandler(HttpHandler next, Consumer<HttpExchange> eventConsumer, DecompressService decompressService) {
        this.next = next;
        this.eventConsumer = eventConsumer;
        this.decompressService = decompressService;
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerRequestExchange) throws Exception {

        final HttpExchange.Builder builder = new HttpExchange.Builder();
        HttpExchange requestExchange = builder
                .requestURI(httpServerRequestExchange.getRequestURI())
                .requestHeaders(headerMapToStringMap(httpServerRequestExchange.getRequestHeaders()))
                .requestCookies(cookiesToMap(httpServerRequestExchange.requestCookies()))
                .queryParameters(queryToMap(httpServerRequestExchange.getQueryParameters()))
                .method(httpServerRequestExchange.getRequestMethod().toString())
                .build();

        eventConsumer.accept(requestExchange);

        httpServerRequestExchange.addExchangeCompleteListener((HttpServerExchange httpServerResponseExchange, ExchangeCompletionListener.NextListener nextListener) -> {
            HttpExchange responseExchange = builder
                    .status(httpServerResponseExchange.getStatusCode())
                    .responseHeaders(headerMapToStringMap(httpServerResponseExchange.getResponseHeaders()))
                    .responseBody(getContentSupplier(
                            StoredResponse.INSTANCE.readAttribute(httpServerResponseExchange),
                            httpServerResponseExchange.getResponseHeaders().get(Headers.CONTENT_ENCODING),
                            httpServerRequestExchange.getRequestCharset()
                    ))
                    .requestBody(getContentSupplier(
                            StoredRequest.INSTANCE.readAttribute(httpServerResponseExchange),
                            httpServerResponseExchange.getRequestHeaders().get(Headers.CONTENT_ENCODING),
                            httpServerResponseExchange.getResponseCharset()
                    ))
                    .responseCookies(cookiesToMap(httpServerResponseExchange.responseCookies()))
                    .end()
                    .build();
            eventConsumer.accept(responseExchange);
            nextListener.proceed();
        });

        next.handleRequest(httpServerRequestExchange);
    }

    private Supplier<String> getContentSupplier(String content, HeaderValues encodings, String charset) {
        return encodings.isEmpty() ? () -> content : () -> decompressService.decompress(content, encodings, charset);
    }

    private Map<String, String> headerMapToStringMap(HeaderMap headerMap) {
        return StreamSupport.stream(headerMap.spliterator(), false)
                .map(values -> new SimpleEntry<>(values.getHeaderName().toString(), values.stream().collect(joining(","))))
                .collect(toMap(Entry::getKey, Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    private Map<String, String> cookiesToMap(Iterable<Cookie> cookies) {
        return StreamSupport.stream(cookies.spliterator(), false)
                .map(cookie -> new SimpleEntry<>(cookie.getName(), cookieAsString(cookie)))
                .collect(toMap(Entry::getKey, Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }

    private static String cookieAsString(Cookie cookie) {
        return cookie.getValue() + "; domain=" + cookie.getDomain() + "; path=" + cookie.getPath();
    }

    private Map<String, String> queryToMap(Map<String, Deque<String>> queryParameters) {
        return queryParameters.entrySet().stream()
                .map(entry -> new SimpleEntry<>(entry.getKey(), entry.getValue().stream().collect(joining(","))))
                .collect(toMap(Entry::getKey, Entry::getValue, (x, y) -> y, LinkedHashMap::new));
    }
}
