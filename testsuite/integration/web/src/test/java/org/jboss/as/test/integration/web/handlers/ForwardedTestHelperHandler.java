package org.jboss.as.test.integration.web.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.jboss.as.test.shared.TestSuiteEnvironment;


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
        String address = exchange.getDestinationAddress().getAddress().toString();
        if(address.startsWith("/")) {
            address = "/" + TestSuiteEnvironment.formatPossibleIpv6Address(address.substring(1));
        } else {
            address = TestSuiteEnvironment.formatPossibleIpv6Address(address);
        }
        String value = exchange.getSourceAddress() + "|" + exchange.getRequestScheme() + "|"
                + address + ':' + exchange.getDestinationAddress().getPort();

        exchange.getResponseHeaders().put(new HttpString(FORWARD_TEST_HEADER), value);
        next.handleRequest(exchange);
    }
}
