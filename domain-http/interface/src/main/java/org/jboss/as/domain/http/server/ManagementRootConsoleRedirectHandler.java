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

import static org.jboss.as.domain.http.server.Common.*;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ManagementRootConsoleRedirectHandler implements HttpHandler {

    private static HttpString HTTP_GET = new HttpString("GET");
    private final ResourceHandlerDefinition consoleHandler;

    ManagementRootConsoleRedirectHandler(ResourceHandlerDefinition consoleHandler) {
        this.consoleHandler = consoleHandler;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (!exchange.getRequestMethod().equals(HTTP_GET)) {
            METHOD_NOT_ALLOWED_HANDLER.handleRequest(exchange);
            return;
        }

        String requestUrl = exchange.getRequestURL();
        if (consoleHandler != null && "/".equals(exchange.getRequestPath())) {
            if (requestUrl.endsWith("/")) {
                requestUrl = requestUrl.substring(0, requestUrl.length() - 1);
            }
            StringBuilder redirect = new StringBuilder(requestUrl);
            redirect.append(consoleHandler.getContext()).append(consoleHandler.getDefaultPath());
            exchange.getResponseHeaders().add(Headers.LOCATION, redirect.toString());
            MOVED_PERMANENTLY.handleRequest(exchange);
            return;
        }
        NOT_FOUND.handleRequest(exchange);
    }
}
