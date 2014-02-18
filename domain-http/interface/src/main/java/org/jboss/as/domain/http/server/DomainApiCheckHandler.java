package org.jboss.as.domain.http.server;

import static org.jboss.as.domain.http.server.Constants.FORBIDDEN;
import static org.jboss.as.domain.http.server.Constants.HOST;
import static org.jboss.as.domain.http.server.Constants.HTTP;
import static org.jboss.as.domain.http.server.Constants.HTTPS;
import static org.jboss.as.domain.http.server.Constants.METHOD_NOT_ALLOWED;
import static org.jboss.as.domain.http.server.Constants.OPTIONS;
import static org.jboss.as.domain.http.server.Constants.ORIGIN;
import static org.jboss.as.domain.http.server.Constants.RETRY_AFTER;
import static org.jboss.as.domain.http.server.Constants.SERVICE_UNAVAILABLE;
import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;

import java.io.IOException;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.domain.http.server.security.SubjectAssociationHandler;
import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpHandler;
import org.jboss.com.sun.net.httpserver.HttpsServer;

/**
 * @author Emanuel Muckenhuber
 */
public class DomainApiCheckHandler implements HttpHandler {

    private final ControlledProcessStateService controlledProcessStateService;
    private final HttpHandler wrapped;

    public DomainApiCheckHandler(final HttpHandler wrapped, final ControlledProcessStateService controlledProcessStateService) {
        this.controlledProcessStateService = controlledProcessStateService;
        // The SubjectAssociationHandler wraps all calls to this HttpHandler to ensure the Subject has been associated
        // with the security context.
        this.wrapped = new SubjectAssociationHandler(wrapped);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!commonChecks(exchange)) {
            return;
        }

        wrapped.handle(exchange);
    }

    protected boolean commonChecks(HttpExchange http) throws IOException {

        // AS7-2284 If we are starting or stopping, tell caller the service is unavailable and to try again
        // later. If "stopping" it's either a reload, in which case trying again will eventually succeed,
        // or it's a true process stop eventually the server will have stopped.
        @SuppressWarnings("deprecation")
        ControlledProcessState.State currentState = controlledProcessStateService.getCurrentState();
        if (currentState == ControlledProcessState.State.STARTING
                || currentState == ControlledProcessState.State.STOPPING) {
            http.getResponseHeaders().add(RETRY_AFTER, "2"); //  2 secs is just a guesstimate
            http.sendResponseHeaders(SERVICE_UNAVAILABLE, -1);
            return false;
        }

        /**
         *  Request Verification - before the request is handled a set of checks are performed for
         *  CSRF and XSS
         */

        /*
         * Completely disallow OPTIONS - if the browser suspects this is a cross site request just reject it.
         */
        final String requestMethod = http.getRequestMethod();
        if (OPTIONS.equals(requestMethod)) {
            drain(http);
            ROOT_LOGGER.debug("Request rejected due to 'OPTIONS' method which is not supported.");
            http.sendResponseHeaders(METHOD_NOT_ALLOWED, -1);

            return false;
        }

        /*
         *  Origin check, if it is set the Origin header should match the Host otherwise reject the request.
         *
         *  This check is for cross site scripted GET and POST requests.
         */
        final Headers headers = http.getRequestHeaders();
        if (headers.containsKey(ORIGIN)) {
            String origin = headers.getFirst(ORIGIN);
            String host = headers.getFirst(HOST);
            String protocol = http.getHttpContext().getServer() instanceof HttpsServer ? HTTPS : HTTP;
            //This browser set header should not need IPv6 escaping
            String allowedOrigin = protocol + "://" + host;

            // This will reject multi-origin Origin headers due to the exact match.
            if (origin.equals(allowedOrigin) == false) {
                drain(http);
                ROOT_LOGGER.debug("Request rejected due to HOST/ORIGIN mis-match.");
                http.sendResponseHeaders(FORBIDDEN, -1);

                return false;
            }
        }
        return true;
    }

    static void drain(HttpExchange exchange) throws IOException {
        try {
            exchange.getRequestBody().close();
        } catch (IOException e) {
            // ignore
        }
    }

}
