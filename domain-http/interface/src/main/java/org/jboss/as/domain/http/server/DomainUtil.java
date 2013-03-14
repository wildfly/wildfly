/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static io.undertow.util.Headers.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;
import org.xnio.streams.ChannelOutputStream;

/**
 * Utility methods used for HTTP based domain management.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DomainUtil {

    public static void writeResponse(HttpServerExchange exchange, boolean isGet, boolean pretty, ModelNode response, int status, boolean encode) {
        final String contentType = encode ? Common.APPLICATION_DMR_ENCODED : Common.APPLICATION_JSON;
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType  + ";" + Common.UTF_8);
        exchange.setResponseCode(status);


        //TODO Content-Length?
        if (isGet && status == 200) {
            response = response.get(RESULT);
        }

        OutputStream out = new ChannelOutputStream(exchange.getResponseChannel());
        PrintWriter print = new PrintWriter(out);
        try {
            try {
                if (encode) {
                    response.writeBase64(out);
                } else {
                    response.writeJSONString(print, !pretty);
                }
            } finally {
                print.flush();
                try {
                    out.flush();
                } finally {
                    IoUtils.safeClose(print);
                    IoUtils.safeClose(out);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Based on the current request represented by the HttpExchange construct a complete URL for the supplied path.
     *
     * @param exchange - The current HttpExchange
     * @param path - The path to include in the constructed URL
     * @return The constructed URL
     */
    public static String constructUrl(final HttpServerExchange exchange, final String path) {
        final HeaderMap headers = exchange.getRequestHeaders();
        String host = headers.getFirst(HOST);
        String protocol = exchange.getConnection().getSslSession() != null ? "https" : "http";

        return protocol + "://" + host + path;
    }

}
