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
package org.jboss.as.domain.http.server.undertow;

import static org.jboss.as.domain.http.server.undertow.UndertowHttpServerLogger.ROOT_LOGGER;
import io.undertow.server.HttpCompletionHandler;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.HttpHandlers;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.blocking.BlockingHandler;
import io.undertow.server.handlers.form.MultiPartHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.com.sun.net.httpserver.Authenticator;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class DomainApiCheckHandler implements HttpHandler {

    static String PATH = "/management";
    private static final String UPLOAD_REQUEST = PATH + "/add-content";

    private final ControlledProcessStateService controlledProcessStateService;
    private final BlockingHandler domainApiHandler;
    private final MultiPartHandler uploadHandler = new MultiPartHandler();;


    DomainApiCheckHandler(final ModelControllerClient modelController, final Authenticator authenticator,
                     final ControlledProcessStateService controlledProcessStateService) {
        this.controlledProcessStateService = controlledProcessStateService;
        domainApiHandler = new BlockingHandler(new DomainApiHandler(modelController));
        uploadHandler.setNext(new BlockingHandler(new DomainApiUploadHandler(modelController)));
    }

    @Override
    public void handleRequest(HttpServerExchange exchange, HttpCompletionHandler completionHandler) {
        if (!commonChecks(exchange, completionHandler)) {
            return;
        }

        boolean isUpload = UPLOAD_REQUEST.equals(exchange.getCanonicalPath());
        if (Methods.POST.equals(exchange.getRequestMethod())) {
            if (isUpload) {
                HttpHandlers.executeHandler(uploadHandler, exchange, completionHandler);
                return;
            }
            if (!checkPostContentType(exchange, completionHandler)) {
                return;
            }

        }

        HttpHandlers.executeHandler(domainApiHandler, exchange, completionHandler);
    }

    private boolean checkPostContentType(HttpServerExchange exchange, HttpCompletionHandler completionHandler) {
        HeaderMap headers = exchange.getRequestHeaders();
        String contentType = extractContentType(headers.getFirst(Headers.CONTENT_TYPE));
        if (!(Common.APPLICATION_JSON.equals(contentType) || Common.APPLICATION_DMR_ENCODED.equals(contentType))) {

            // RFC 2616: 14.11 Content-Encoding
            // If the content-coding of an entity in a request message is not
            // acceptable to the origin server, the server SHOULD respond with a
            // status code of 415 (Unsupported Media Type).
            ROOT_LOGGER.debug("Request rejected due to unsupported media type - should be one of (application/json,application/dmr-encoded).");
            HttpHandlers.executeHandler(Common.UNSUPPORTED_MEDIA_TYPE, exchange, completionHandler);
            return false;
        }
        return true;
    }

    private String extractContentType(final String fullContentType) {
        int pos = fullContentType.indexOf(';');
        return pos < 0 ? fullContentType : fullContentType.substring(0, pos).trim();
    }

    private boolean commonChecks(HttpServerExchange exchange, HttpCompletionHandler completionHandler) {
     // AS7-2284 If we are starting or stopping, tell caller the service is unavailable and to try again
        // later. If "stopping" it's either a reload, in which case trying again will eventually succeed,
        // or it's a true process stop eventually the server will have stopped.
        @SuppressWarnings("deprecation")
        ControlledProcessState.State currentState = controlledProcessStateService.getCurrentState();
        if (currentState == ControlledProcessState.State.STARTING
                || currentState == ControlledProcessState.State.STOPPING) {
            exchange.getResponseHeaders().add(Headers.RETRY_AFTER, "2"); //  2 secs is just a guesstimate
            HttpHandlers.executeHandler(Common.SERVICE_UNAVAIABLE, exchange, completionHandler);
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
            HttpHandlers.executeHandler(Common.METHOD_NOT_ALLOWED_HANDLER, exchange, completionHandler);
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
                HttpHandlers.executeHandler(ResponseCodeHandler.HANDLE_403, exchange, completionHandler);
                return false;
            }
        }
        return true;
    }
}
