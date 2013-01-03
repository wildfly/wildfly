/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.domain.http.server.Constants.APPLICATION_DMR_ENCODED;
import static org.jboss.as.domain.http.server.Constants.APPLICATION_JSON;
import static org.jboss.as.domain.http.server.Constants.CONTENT_TYPE;
import static org.jboss.as.domain.http.server.Constants.HOST;
import static org.jboss.as.domain.http.server.Constants.HTTP;
import static org.jboss.as.domain.http.server.Constants.HTTPS;
import static org.jboss.as.domain.http.server.Constants.OK;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.jboss.com.sun.net.httpserver.Headers;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpsServer;
import org.jboss.dmr.ModelNode;

/**
 * A utility class to hold common functionality for handling the DMR HTTP exchanges.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class DomainUtil {

    // Prevent Instantiation
    private DomainUtil() {
    }

    /**
     * Writes the HTTP response to the output stream.
     *
     * @param http The HttpExchange object that allows access to the request and response.
     * @param isGet Flag indicating whether or not the request was a GET request or POST request.
     * @param pretty Flag indicating whether or not the output, if JSON, should be pretty printed or not.
     * @param response The DMR response from the operation.
     * @param status The HTTP status code to be included in the response.
     * @param encode Flag indicating whether or not to Base64 encode the response payload.
     * @throws IOException if an error occurs while attempting to generate the HTTP response.
     */
    static void writeResponse(final HttpExchange http, boolean isGet, boolean pretty, ModelNode response, int status,
            boolean encode, String contentType) throws IOException {
        final Headers responseHeaders = http.getResponseHeaders();
        responseHeaders.add(CONTENT_TYPE, contentType);
        http.sendResponseHeaders(status, 0);

        // GET (read) operations will never have a compensating update, and the status is already
        // available via the http response status code, so unwrap them.
        if (isGet && status == OK) {
            response = response.get("result");
        }

        final OutputStream out = http.getResponseBody();
        final PrintWriter print = new PrintWriter(out);

        try {
            if (encode) {
                response.writeBase64(out);
            } else {
                response.writeJSONString(print, !pretty);
            }
        } finally {
            print.flush();
            out.flush();
            safeClose(print);
            safeClose(out);
        }
    }

    static void writeResponse(final HttpExchange http, boolean isGet, boolean pretty, ModelNode response, int status,
            boolean encode) throws IOException {
         String contentType = encode ? APPLICATION_DMR_ENCODED : APPLICATION_JSON;
         writeResponse(http, isGet, pretty, response, status, encode, contentType);
     }

    static void safeClose(Closeable close) {
        try {
            close.close();
        } catch (Throwable eat) {
        }
    }

    /**
     * Based on the current request represented by the HttpExchange construct a complete URL for the supplied path.
     *
     * @param exchange - The current HttpExchange
     * @param path - The path to include in the constructed URL
     * @return The constructed URL
     */
    static String constructUrl(final HttpExchange exchange, final String path) {
        final Headers headers = exchange.getRequestHeaders();
        String host = headers.getFirst(HOST);
        String protocol = exchange.getHttpContext().getServer() instanceof HttpsServer ? HTTPS : HTTP;

        return protocol + "://" + host + path;
    }

}
