/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.domain.http.server.Constants.MOVED_PERMENANTLY;

import java.io.IOException;
import java.net.URI;

import org.jboss.com.sun.net.httpserver.Filter;
import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;

/**
 * Filter to ensure request to a context have a trailing slash.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class TrailingSlashFilter extends Filter {

    private static final String LOCATION = "Location";

    /**
     * Sent a MOVED_PERMENANTLY response for requests that omit a trailing slash.
     *
     * @see com.sun.net.httpserver.Filter#doFilter(com.sun.net.httpserver.HttpExchange, com.sun.net.httpserver.Filter.Chain)
     */
    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        URI requestURI = exchange.getRequestURI();
        String path = requestURI.getPath();

        if (path.endsWith("/") == false) {
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.add(LOCATION, path + "/");
            exchange.sendResponseHeaders(MOVED_PERMENANTLY, 0);
            exchange.close();
        } else {
            chain.doFilter(exchange);
        }
    }

    /**
     * @see com.sun.net.httpserver.Filter#description()
     */
    @Override
    public String description() {
        return "Ensure all requests to the context have a trailing slash.";
    }

}
