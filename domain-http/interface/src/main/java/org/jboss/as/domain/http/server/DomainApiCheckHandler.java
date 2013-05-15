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

import java.util.Arrays;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.form.MultiPartHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.domain.http.server.security.SubjectAssociationHandler;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class DomainApiCheckHandler implements HttpHandler {

    static String PATH = "/management";
    private static final long ONE_DAY_IN_SECONDS = 24 * 60 * 60;
    private static final String UPLOAD_REQUEST = PATH + "/add-content";

    private final ControlledProcessStateService controlledProcessStateService;
    private final HttpHandler domainApiHandler;
    private final MultiPartHandler uploadHandler = new MultiPartHandler();;


    DomainApiCheckHandler(final ModelControllerClient modelController, final ControlledProcessStateService controlledProcessStateService) {
        this.controlledProcessStateService = controlledProcessStateService;
        domainApiHandler = new BlockingHandler(new SubjectAssociationHandler(new DomainApiHandler(modelController)));
        uploadHandler.setNext(new BlockingHandler(new SubjectAssociationHandler(new DomainApiUploadHandler(modelController))));
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (!commonChecks(exchange)) {
            return;
        }

        /*
         * Allow OPTIONS as part of CORS preflighted requests
         * TODO Should there be a list of trusted origins when handling CORS requests?
         */
        HeaderMap requestHeaders = exchange.getRequestHeaders();
        if (Methods.OPTIONS.equals(exchange.getRequestMethod())) {
            if (!requestHeaders.contains(Headers.ORIGIN)) {
                ROOT_LOGGER.debug("Request rejected due to 'OPTIONS' method without an origin.");
                Common.METHOD_NOT_ALLOWED_HANDLER.handleRequest(exchange);
                return;
            }
            addOptionsHeader(requestHeaders.getFirst(Headers.ORIGIN), exchange.getResponseHeaders());
            exchange.setResponseCode(200);
            return;
        }

        /*
         * Add the origin from the request headers as allowed origin to the response header.
         * None CORS requests will just ignore this and continue to work.
         */
        if (requestHeaders.contains(Headers.ORIGIN)) {
            String origin = requestHeaders.getFirst(Headers.ORIGIN);
            HeaderMap responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add(HttpString.tryFromString("Access-Control-Allow-Origin"), origin);
            responseHeaders.add(HttpString.tryFromString("Access-Control-Allow-Credentials"), "true");
        }

        boolean isUpload = UPLOAD_REQUEST.equals(exchange.getCanonicalPath());
        if (Methods.POST.equals(exchange.getRequestMethod())) {
            if (isUpload) {
                uploadHandler.handleRequest(exchange);
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
         * Disallow anything but GET, POST and OPTIONS
         */
        final HttpString requestMethod = exchange.getRequestMethod();
        if (!Methods.POST.equals(requestMethod) && !Methods.GET.equals(requestMethod) && !Methods.OPTIONS.equals(requestMethod)) {
            ROOT_LOGGER.debug("Request rejected as method not one of (GET,POST,OPTIONS).");
            Common.METHOD_NOT_ALLOWED_HANDLER.handleRequest(exchange);
            return false;
        }

        return true;
    }

    private void addOptionsHeader(final String origin, final HeaderMap responseHeaders) {
        responseHeaders.add(HttpString.tryFromString("Access-Control-Allow-Origin"), origin);
        responseHeaders.addAll(HttpString.tryFromString("Access-Control-Allow-Methods"),
                Arrays.asList(Methods.GET_STRING, Methods.POST_STRING, Methods.OPTIONS_STRING));
        responseHeaders.add(HttpString.tryFromString("Access-Control-Allow-Headers"), "Authorization");
        responseHeaders.add(HttpString.tryFromString("Access-Control-Allow-Headers"), "WWW-Authenticate");
        responseHeaders.add(HttpString.tryFromString("Access-Control-Allow-Headers"), "Content-Type");
        responseHeaders.add(HttpString.tryFromString("Access-Control-Allow-Credentials"), "true");
        responseHeaders.add(HttpString.tryFromString("Access-Control-Max-Age"), String.valueOf(ONE_DAY_IN_SECONDS));
        responseHeaders.add(HttpString.tryFromString("Content-Length"), "0");
        responseHeaders.add(HttpString.tryFromString("Content-Type"), "text/plain");
    }
}
