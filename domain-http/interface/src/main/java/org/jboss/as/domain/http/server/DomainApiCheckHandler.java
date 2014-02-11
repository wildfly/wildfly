/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.domain.http.server;

import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.ModelController;
import org.jboss.as.domain.http.server.security.SubjectDoAsHandler;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class DomainApiCheckHandler implements HttpHandler {

    static String PATH = "/management";
    private static final String GENERIC_CONTENT_REQUEST = PATH + "-upload";
    private static final String ADD_CONTENT_REQUEST = PATH + "/add-content";

    private final ControlledProcessStateService controlledProcessStateService;
    private final HttpHandler domainApiHandler;
    private final HttpHandler addContentHandler;
    private final HttpHandler genericOperationHandler;


    DomainApiCheckHandler(final ModelController modelController, final ControlledProcessStateService controlledProcessStateService) {
        this.controlledProcessStateService = controlledProcessStateService;
        domainApiHandler = new BlockingHandler(new SubjectDoAsHandler(new DomainApiHandler(modelController)));
        addContentHandler = new BlockingHandler(new SubjectDoAsHandler(new DomainApiUploadHandler(modelController)));
        genericOperationHandler = new BlockingHandler(new SubjectDoAsHandler(new DomainApiGenericOperationHandler(modelController)));
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (!commonChecks(exchange)) {
            return;
        }

        if (Methods.POST.equals(exchange.getRequestMethod())) {
            boolean isAddContent = ADD_CONTENT_REQUEST.equals(exchange.getRequestPath());
            boolean isGeneric = GENERIC_CONTENT_REQUEST.equals(exchange.getRequestPath());
            if (isAddContent) {
                addContentHandler.handleRequest(exchange);
                return;
            } else if (isGeneric) {
                genericOperationHandler.handleRequest(exchange);
                return;
            }
            if (!checkPostContentType(exchange)) {
                return;
            }
        }

        domainApiHandler.handleRequest(exchange);
    }

    private boolean checkPostContentType(HttpServerExchange exchange) throws Exception {
        HeaderMap headers = exchange.getRequestHeaders();
        String contentType = extractContentType(headers.getFirst(Headers.CONTENT_TYPE));
        if (!(Common.APPLICATION_JSON.equals(contentType) || Common.APPLICATION_DMR_ENCODED.equals(contentType))) {

            // RFC 2616: 14.11 Content-Encoding
            // If the content-coding of an entity in a request message is not
            // acceptable to the origin server, the server SHOULD respond with a
            // status code of 415 (Unsupported Media Type).
            ROOT_LOGGER.debug("Request rejected due to unsupported media type - should be one of (application/json,application/dmr-encoded).");
            Common.UNSUPPORTED_MEDIA_TYPE.handleRequest(exchange);
            return false;
        }
        return true;
    }

    private String extractContentType(final String fullContentType) {
        if (fullContentType == null) {
            return "";
        }
        int pos = fullContentType.indexOf(';');
        return pos < 0 ? fullContentType : fullContentType.substring(0, pos).trim();
    }

    private boolean commonChecks(HttpServerExchange exchange) throws Exception {
        // AS7-2284 If we are starting or stopping, tell caller the service is unavailable and to try again
        // later. If "stopping" it's either a reload, in which case trying again will eventually succeed,
        // or it's a true process stop eventually the server will have stopped.
        @SuppressWarnings("deprecation")
        ControlledProcessState.State currentState = controlledProcessStateService.getCurrentState();
        if (currentState == ControlledProcessState.State.STARTING
                || currentState == ControlledProcessState.State.STOPPING) {
            exchange.getResponseHeaders().add(Headers.RETRY_AFTER, "2"); //  2 secs is just a guesstimate
            Common.SERVICE_UNAVAIABLE.handleRequest(exchange);
            return false;
        }

        /*
         * Completely disallow OPTIONS - if the browser suspects this is a cross site request just reject it.
         */
        final HttpString requestMethod = exchange.getRequestMethod();
        if (!Methods.POST.equals(requestMethod) && !Methods.GET.equals(requestMethod)) {
            if (Methods.OPTIONS.equals(requestMethod)) {
                ROOT_LOGGER.debug("Request rejected due to 'OPTIONS' method which is not supported.");
            } else {
                ROOT_LOGGER.debug("Request rejected as method not one of (GET,POST).");
            }
            Common.METHOD_NOT_ALLOWED_HANDLER.handleRequest(exchange);
            return false;
        }

        /*
         *  Origin check, if it is set the Origin header should match the Host otherwise reject the request.
         *
         *  This check is for cross site scripted GET and POST requests.
         */
        final HeaderMap headers = exchange.getRequestHeaders();
        if (headers.contains(Headers.ORIGIN)) {
            String origin = headers.getFirst(Headers.ORIGIN);
            String host = headers.getFirst(Headers.HOST);
            String protocol = exchange.getRequestScheme();
            //This browser set header should not need IPv6 escaping
            String allowedOrigin = protocol + "://" + host;

            // This will reject multi-origin Origin headers due to the exact match.
            if (origin.equals(allowedOrigin) == false) {
                ROOT_LOGGER.debug("Request rejected due to HOST/ORIGIN mis-match.");
                ResponseCodeHandler.HANDLE_403.handleRequest(exchange);
                return false;
            }
        }
        return true;
    }
}
