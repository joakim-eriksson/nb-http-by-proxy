package se.jocke.nb.http.bp.core.service;

import io.undertow.UndertowLogger;
import io.undertow.attribute.ExchangeAttribute;
import io.undertow.attribute.ReadOnlyAttributeException;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class StoredRequest implements ExchangeAttribute {

    public static final ExchangeAttribute INSTANCE = new StoredRequest();

    @Override
    public String readAttribute(HttpServerExchange exchange) {
        byte[] data = exchange.getAttachment(RequestConduitWrapper.REQUEST);

        if (data == null || data.length == 0) {
            return null;
        }

        String charset = extractCharset(exchange.getRequestHeaders());
        if (charset == null) {
            return null;
        }
        try {
            return new String(data, charset);
        } catch (UnsupportedEncodingException e) {
            UndertowLogger.ROOT_LOGGER.debugf(e, "Could not decode request body using charset %s", charset);
            return null;
        }
    }

    private String extractCharset(HeaderMap headers) {
        String contentType = headers.getFirst(Headers.CONTENT_TYPE);
        if (contentType != null) {
            String value = Headers.extractQuotedValueFromHeader(contentType, "charset");
            if (value != null) {
                return value;
            }
            if (contentType.startsWith("text/")) {
                return StandardCharsets.ISO_8859_1.displayName();
            } else if (contentType.endsWith("json")) {
                return StandardCharsets.UTF_8.displayName();
            }
            return null;
        }
        return null;
    }

    @Override
    public void writeAttribute(HttpServerExchange exchange, String newValue) throws ReadOnlyAttributeException {
        throw new ReadOnlyAttributeException("Stored Request", newValue);
    }
}
