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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import org.jboss.dmr.ModelNode;

/**
 * Utility methods used for HTTP based domain management.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class DomainUtil {

    public static void writeResponse(final HttpServerExchange exchange, final int status, ModelNode response,
            OperationParameter operationParameter) {

        exchange.setResponseCode(status);

        final HeaderMap responseHeaders = exchange.getResponseHeaders();
        final String contentType = operationParameter.isEncode() ? Common.APPLICATION_DMR_ENCODED : Common.APPLICATION_JSON;
        responseHeaders.put(Headers.CONTENT_TYPE, contentType + "; charset=" + Common.UTF_8);

        writeCacheHeaders(exchange, status, operationParameter);

        if (operationParameter.isGet() && status == 200) {
            // For GET request the response is purley the model nodes result. The outcome
            // is not send as part of the response but expressed with the HTTP status code.
            response = response.get(RESULT);
        }
        try {
            byte[] data = getResponseBytes(response, operationParameter);
            responseHeaders.put(Headers.CONTENT_LENGTH, data.length);
            exchange.getResponseSender().send(ByteBuffer.wrap(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] getResponseBytes(final ModelNode modelNode, final OperationParameter operationParameter) throws IOException {
        if (operationParameter.isEncode()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(baos);
            modelNode.writeBase64(out);
            out.flush();
            return baos.toByteArray();
        } else {
            String json = modelNode.toJSONString(!operationParameter.isPretty());
            return json.getBytes(Common.UTF_8);
        }
    }

    public static void writeCacheHeaders(final HttpServerExchange exchange, final int status, final OperationParameter operationParameter) {
        final HeaderMap responseHeaders = exchange.getResponseHeaders();

        // No need to send this in a 304
        // See http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5
        if (operationParameter.getMaxAge() > 0 && status != 304) {
            responseHeaders.put(Headers.CACHE_CONTROL, "max-age=" + operationParameter.getMaxAge() + ", private, must-revalidate");
        }
        if (operationParameter.getEtag() != null) {
            responseHeaders.put(Headers.ETAG, operationParameter.getEtag().toString());
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
        String protocol = exchange.getConnection().getSslSessionInfo() != null ? "https" : "http";

        return protocol + "://" + host + path;
    }
}
