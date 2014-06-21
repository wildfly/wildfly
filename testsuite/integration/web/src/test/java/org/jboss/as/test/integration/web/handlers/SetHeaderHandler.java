package org.jboss.as.test.integration.web.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;


/**
 * @author Stuart Douglas
 */
public class SetHeaderHandler implements HttpHandler {

    private final HttpHandler next;
    private String name;
    private String value;

    public SetHeaderHandler(HttpHandler next) {
        this.next = next;
    }


    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(new HttpString(name), value);
        next.handleRequest(exchange);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
