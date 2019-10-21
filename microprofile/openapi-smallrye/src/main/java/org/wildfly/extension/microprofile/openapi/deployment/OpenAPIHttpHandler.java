/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.openapi.deployment;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger;

import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.smallrye.openapi.runtime.io.OpenApiSerializer.Format;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

/**
 * {@link HttpHandler} for the Open API endpoint.
 * @author Michael Edgar
 * @author Paul Ferraro
 */
public class OpenAPIHttpHandler implements HttpHandler, Function<Format, String> {

    private static final String ALLOW_METHODS = String.join(",", Methods.GET_STRING, Methods.HEAD_STRING, Methods.OPTIONS_STRING);
    private static final String DEFAULT_ALLOW_HEADERS = String.join(",", Headers.CONTENT_TYPE_STRING, Headers.AUTHORIZATION_STRING);
    private static final long DEFAULT_MAX_AGE = ChronoUnit.DAYS.getDuration().getSeconds();
    private static final String FORMAT = "format";
    private static final Set<String> ACCEPT_ANY = new TreeSet<>(Arrays.asList("*/*", "application/*"));

    private final Map<Format, String> documents = new ConcurrentHashMap<>();
    private final OpenAPI model;

    public OpenAPIHttpHandler(OpenAPI model) {
        this.model = model;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        HttpString requestMethod = exchange.getRequestMethod();
        HeaderMap responseHeaders = exchange.getResponseHeaders();

        // Add CORS response headers
        responseHeaders.put(new HttpString("Access-Control-Allow-Origin"), "*");
        responseHeaders.put(new HttpString("Access-Control-Allow-Credentials"), "true");
        responseHeaders.put(new HttpString("Access-Control-Allow-Methods"), ALLOW_METHODS);
        responseHeaders.put(new HttpString("Access-Control-Allow-Headers"), DEFAULT_ALLOW_HEADERS);
        responseHeaders.put(new HttpString("Access-Control-Max-Age"), DEFAULT_MAX_AGE);

        if (requestMethod.equals(Methods.GET) || requestMethod.equals(Methods.HEAD)) {
            // Default content type is YAML
            Format format = Format.YAML;

            // Check Accept, then query parameter "format" for JSON; else use YAML.
            String accept = exchange.getRequestHeaders().getFirst(Headers.ACCEPT);
            if ((accept != null) && !ACCEPT_ANY.contains(accept)) {
                if (accept.contains(Format.JSON.getMimeType())) {
                    format = Format.JSON;
                }
            } else {
                Deque<String> formatValues = exchange.getQueryParameters().get(FORMAT);
                String formatValue = (formatValues != null) ? formatValues.peek() : null;
                if (formatValue != null) {
                    if (formatValue.equals(Format.JSON.name())) {
                        format = Format.JSON;
                    }
                }
            }

            String result = this.documents.computeIfAbsent(format, this);

            responseHeaders.put(Headers.CONTENT_TYPE, format.getMimeType());
            responseHeaders.put(Headers.CONTENT_LENGTH, result.length());

            if (requestMethod.equals(Methods.GET)) {
                exchange.getResponseSender().send(result);
            }
        } else if (requestMethod.equals(Methods.OPTIONS)) {
            responseHeaders.put(Headers.ALLOW, ALLOW_METHODS);
        } else {
            exchange.setStatusCode(StatusCodes.METHOD_NOT_ALLOWED);
        }
    }

    @Override
    public String apply(Format format) {
        try {
            return OpenApiSerializer.serialize(this.model, format);
        } catch (IOException e) {
            MicroProfileOpenAPILogger.LOGGER.failedToSerializeDocument(e, format);
            return null;
        }
    }
}
