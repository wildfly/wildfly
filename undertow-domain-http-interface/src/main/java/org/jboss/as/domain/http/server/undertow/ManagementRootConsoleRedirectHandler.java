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

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.HttpHandlers;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ManagementRootConsoleRedirectHandler implements HttpHandler {

    private static HttpString HTTP_GET = new HttpString("GET");
    private final ResourceHandler consoleHandler;

    ManagementRootConsoleRedirectHandler(ResourceHandler consoleHandler) {
        this.consoleHandler = consoleHandler;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        if (!exchange.getRequestMethod().equals(HTTP_GET)) {
            HttpHandlers.executeHandler(Common.METHOD_NOT_ALLOWED_HANDLER, exchange);
            return;
        }

        if (consoleHandler != null) {
            String requestUrl = exchange.getRequestURL();
            if (requestUrl.charAt(requestUrl.length() - 1) == '/') {
                requestUrl = requestUrl.substring(0, requestUrl.length() - 1);
            }
            StringBuilder redirect = new StringBuilder(requestUrl);
            redirect.append(consoleHandler.getDefaultPath());
            exchange.getResponseHeaders().add(Headers.LOCATION, redirect.toString());

            HttpHandlers.executeHandler(Common.MOVED_PERMANENTLY, exchange);
        } else {
            HttpHandlers.executeHandler(ResponseCodeHandler.HANDLE_404, exchange);
        }
    }
}
