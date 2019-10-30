package org.jboss.as.test.integration.web.security.external;

import io.undertow.security.impl.ExternalAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * @author Stuart Douglas
 */
public class UserHandler implements HttpHandler {

    private final HttpHandler next;

    public UserHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String user = exchange.getRequestHeaders().getFirst("User");
        if(user != null) {
            exchange.putAttachment(ExternalAuthenticationMechanism.EXTERNAL_PRINCIPAL, user);
            exchange.putAttachment(ExternalAuthenticationMechanism.EXTERNAL_AUTHENTICATION_TYPE, "user-header");
        }
        next.handleRequest(exchange);
    }
}
