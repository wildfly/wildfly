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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.resteasy.util.AcceptParser;

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
public class OpenAPIHttpHandler implements HttpHandler {

    private static final String ALLOW_METHODS = String.join(",", Methods.GET_STRING, Methods.HEAD_STRING, Methods.OPTIONS_STRING);
    private static final String DEFAULT_ALLOW_HEADERS = String.join(",", Headers.CONTENT_TYPE_STRING, Headers.AUTHORIZATION_STRING);
    private static final long DEFAULT_MAX_AGE = ChronoUnit.DAYS.getDuration().getSeconds();
    private static final Map<MediaType, Format> ACCEPTED_TYPES = new LinkedHashMap<>();
    private static final Map<String, Format> FORMATS = new HashMap<>();
    private static final String FORMAT = "format";

    static {
        for (Format format : EnumSet.allOf(Format.class)) {
            ACCEPTED_TYPES.put(MediaType.valueOf(format.getMimeType()), format);
            FORMATS.put(format.name(), format);
        }
    }

    private final OpenAPI model;

    public OpenAPIHttpHandler(OpenAPI model) {
        this.model = model;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws IOException {
        HttpString requestMethod = exchange.getRequestMethod();
        HeaderMap responseHeaders = exchange.getResponseHeaders();

        // Add CORS response headers
        responseHeaders.put(new HttpString("Access-Control-Allow-Origin"), "*");
        responseHeaders.put(new HttpString("Access-Control-Allow-Credentials"), "true");
        responseHeaders.put(new HttpString("Access-Control-Allow-Methods"), ALLOW_METHODS);
        responseHeaders.put(new HttpString("Access-Control-Allow-Headers"), DEFAULT_ALLOW_HEADERS);
        responseHeaders.put(new HttpString("Access-Control-Max-Age"), DEFAULT_MAX_AGE);

        if (requestMethod.equals(Methods.GET) || requestMethod.equals(Methods.HEAD)) {
            // Determine preferred media type
            List<MediaType> preferredTypes = Collections.emptyList();
            List<MediaType> types = parseAcceptedTypes(exchange);

            for (MediaType type : types) {
                List<MediaType> compatibleTypes = new ArrayList<>(ACCEPTED_TYPES.size());
                for (MediaType acceptedType : ACCEPTED_TYPES.keySet()) {
                    if (type.isCompatible(acceptedType)) {
                        compatibleTypes.add(acceptedType);
                    }
                }
                if (!compatibleTypes.isEmpty()) {
                    preferredTypes = compatibleTypes;
                    break;
                }
            }

            // Determine preferred charset
            Charset charset = parseCharset(exchange);

            if (preferredTypes.isEmpty() || (charset == null)) {
                exchange.setStatusCode(StatusCodes.NOT_ACCEPTABLE);
                return;
            }

            // Use format preferred by Accept header if unambiguous, otherwise determine format from query parameter
            Format format = (preferredTypes.size() == 1) ? ACCEPTED_TYPES.get(preferredTypes.get(0)) : parseFormatParameter(exchange);

            String result = OpenApiSerializer.serialize(this.model, format);

            responseHeaders.put(Headers.CONTENT_TYPE, format.getMimeType());
            responseHeaders.put(Headers.CONTENT_LENGTH, result.length());

            if (requestMethod.equals(Methods.GET)) {
                exchange.getResponseSender().send(result, charset);
            }
        } else if (requestMethod.equals(Methods.OPTIONS)) {
            responseHeaders.put(Headers.ALLOW, ALLOW_METHODS);
        } else {
            exchange.setStatusCode(StatusCodes.METHOD_NOT_ALLOWED);
        }
    }

    private static final Comparator<MediaType> MEDIA_TYPE_SORTER = new Comparator<MediaType>() {
        @Override
        public int compare(MediaType type1, MediaType type2) {
            float quality1 = Float.parseFloat(type1.getParameters().getOrDefault("q", "1"));
            float quality2 = Float.parseFloat(type2.getParameters().getOrDefault("q", "1"));
            return Float.compare(quality1, quality2);
        }
    };

    private static List<MediaType> parseAcceptedTypes(HttpServerExchange exchange) {
        String headerValue = exchange.getRequestHeaders().getFirst(Headers.ACCEPT);
        if (headerValue == null) return Collections.singletonList(MediaType.WILDCARD_TYPE);

        List<String> values = AcceptParser.parseAcceptHeader(headerValue);
        List<MediaType> types = new ArrayList<>(values.size());
        for (String value : values) {
            types.add(MediaType.valueOf(value));
        }
        // Sort media types by quality
        Collections.sort(types, MEDIA_TYPE_SORTER);
        return types;
    }

    private static Charset parseCharset(HttpServerExchange exchange) {
        String headerValue = exchange.getRequestHeaders().getFirst(Headers.ACCEPT_CHARSET);
        if (headerValue == null) return StandardCharsets.UTF_8;

        List<String> values = AcceptParser.parseAcceptHeader(headerValue);
        Charset defaultCharset = null;
        for (String value : values) {
            if (value.equals(MediaType.MEDIA_TYPE_WILDCARD)) {
                defaultCharset = StandardCharsets.UTF_8;
            }
            if (Charset.isSupported(value)) {
                return Charset.forName(value);
            }
        }
        return defaultCharset;
    }

    private static Format parseFormatParameter(HttpServerExchange exchange) {
        Deque<String> formatValues = exchange.getQueryParameters().get(FORMAT);
        String formatValue = (formatValues != null) ? formatValues.peek() : null;
        Format format = (formatValue != null) ? FORMATS.get(formatValue) : null;
        // Default format is YAML
        return (format != null) ? format : Format.YAML;
    }
}
