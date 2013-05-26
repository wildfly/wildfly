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
import static org.jboss.as.domain.http.server.HttpServerLogger.ROOT_LOGGER;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.jboss.dmr.ModelNode;
import org.xnio.IoUtils;
import org.xnio.streams.ChannelOutputStream;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.DateUtils;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

/**
 * Utility methods used for HTTP based domain management.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DomainUtil {

    public static void writeResponse(HttpServerExchange exchange, final int status, final ModelNode response,
            OperationParameter operationParameter) {

        exchange.setResponseCode(status);

        final HeaderMap responseHeaders = exchange.getResponseHeaders();
        final String contentType = operationParameter.isEncode() ? Common.APPLICATION_DMR_ENCODED : Common.APPLICATION_JSON;
        responseHeaders.put(Headers.CONTENT_TYPE, contentType + ";" + Common.UTF_8);

        writeCacheHeaders(exchange, operationParameter);

        if (operationParameter.isGet() && status == 200) {
            // Why was only the RESULT part sent and not the complete model node?
            // The admin console *always* expects complete model nodes!
            // response = response.get(RESULT);
            try {
                int length = getResponseLength(response, operationParameter);
                responseHeaders.put(Headers.CONTENT_LENGTH, length);
            } catch (IOException e) {
                ROOT_LOGGER.errorf(e, "Unable to get length for '%s'", operationParameter);
            }
        }

        OutputStream out = new ChannelOutputStream(exchange.getResponseChannel());
        PrintWriter print = new PrintWriter(out);
        try {
            try {
                if (operationParameter.isEncode()) {
                    response.writeBase64(out);
                } else {
                    response.writeJSONString(print, !operationParameter.isPretty());
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

    private static int getResponseLength(final ModelNode modelNode, final OperationParameter operationParameter) throws IOException {
        int length;
        if (operationParameter.isEncode()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(baos);
            modelNode.writeBase64(out);
            out.flush();
            length = baos.size();
            IoUtils.safeClose(out, baos);
        } else {
            String json = modelNode.toJSONString(!operationParameter.isPretty());
            length = json.length();
        }
        return length;
    }

    public static void writeCacheHeaders(final HttpServerExchange exchange, final OperationParameter operationParameter) {
        final HeaderMap responseHeaders = exchange.getResponseHeaders();
        if (operationParameter.getMaxAge() > 0) {
            responseHeaders.put(Headers.CACHE_CONTROL, "max-age=" + operationParameter.getMaxAge() + ", private, must-revalidate");
        }
        if (operationParameter.getLastModified() != null) {
            responseHeaders.put(Headers.LAST_MODIFIED, DateUtils.toDateString(operationParameter.getLastModified()));
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
