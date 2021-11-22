package se.jocke.nb.http.bp.core.node;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import se.jocke.nb.http.bp.core.HttpExchange;

public class HttpExchangeNode extends AbstractNode {

    private final HttpExchange exchange;

    public static final HttpExchangeNode EMPTY_EXCHANGE_NODE = new HttpExchangeNode(HttpExchange.EMPTY);

    public HttpExchangeNode(HttpExchange exchange) {
        super(Children.LEAF);
        setDisplayName("HTTP Exchange");
        this.exchange = exchange;
    }

    @Override
    public Node.PropertySet[] getPropertySets() {
        return new PropertySet[]{
            propertySetFromMap("Query parameters", exchange.getQueryParameters()),
            propertySetFromMap("Request headers", exchange.getRequestHeaders()),
            propertySetFromMap("Request cookies", exchange.getRequestCookies()),
            propertySetFromMap("Response headers", exchange.getResponseHeaders()),
            propertySetFromMap("Response cookies", exchange.getResponseCookies())
        };
    }

    private Sheet.Set propertySetFromMap(String name, Map<String, String> map) {
        Sheet.Set set = Sheet.createPropertiesSet();
        set.setDisplayName(name);
        set.setName(name);
        set.put(createFromMap(map));
        return set;
    }

    private PropertySupport<?>[] createFromMap(Map<String, String> map) {
        return (map.isEmpty() ? Collections.singletonMap("empty", "") : map).entrySet().stream().map(entry -> {
            return new PropertySupport.ReadOnly<String>(entry.getKey(), String.class, entry.getKey(), entry.getKey()) {
                @Override
                public String getValue() throws IllegalAccessException, InvocationTargetException {
                    return entry.getValue();
                }
            };
        }).toArray(PropertySupport[]::new);
    }
}
