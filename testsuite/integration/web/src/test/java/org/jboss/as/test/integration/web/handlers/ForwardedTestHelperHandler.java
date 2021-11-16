package org.jboss.as.test.integration.web.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;


/**
 * @author Jan Stourac
 */
public class ForwardedTestHelperHandler implements HttpHandler {

    public static final String FORWARD_TEST_HEADER = "forwarded-test-header";

    private final HttpHandler next;

    public ForwardedTestHelperHandler(HttpHandler next) {
        this.next = next;
    }


    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String value = exchange.getSourceAddress() + "|" + exchange.getRequestScheme() + "|"
                + formatPossibleIpv6Address(exchange.getDestinationAddress().toString());

        exchange.getResponseHeaders().put(new HttpString(FORWARD_TEST_HEADER), value);
        next.handleRequest(exchange);
    }

    public static String formatPossibleIpv6Address(String address) {
        if (address == null) {
            return address;
        }
        if (!address.contains(":")) {
            return address;
        }
        if (address.startsWith("[") && address.endsWith("]")) {
            return address;
        }
        return "[" + address + "]";
    }
}
