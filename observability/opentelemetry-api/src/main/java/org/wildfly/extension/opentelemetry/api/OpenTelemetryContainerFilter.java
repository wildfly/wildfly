/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.opentelemetry.api;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

@Provider
public class OpenTelemetryContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {
    public static final String SERVER_SPAN = "otel_server_span";
    @Inject
    private OpenTelemetry openTelemetry;
    @Inject
    private Tracer tracer;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (tracer != null) {
            final boolean remoteContext = requestContext.getHeaders().keySet().stream().anyMatch(k -> W3CTraceContextPropagator.getInstance().fields().contains(k));

            final UriInfo uriInfo = requestContext.getUriInfo();
            final URI requestUri = uriInfo.getRequestUri();
            final String method = requestContext.getMethod();
            final String uri = uriInfo.getPath();

            SpanBuilder spanBuilder = tracer.spanBuilder(method + " " + uri)
                    .setSpanKind(SpanKind.SERVER);

            if (remoteContext) {
                spanBuilder.setParent(openTelemetry.getPropagators()
                        .getTextMapPropagator()
                        .extract(Context.current(), requestContext, new TextMapGetter<ContainerRequestContext>() {
                            @Override
                            public String get(ContainerRequestContext requestContext, String key) {
                                if (requestContext.getHeaders().containsKey(key)) {
                                    return requestContext.getHeaders().get(key).get(0);
                                }
                                return null;
                            }

                            @Override
                            public Iterable<String> keys(ContainerRequestContext requestContext) {
                                return requestContext.getHeaders().keySet();
                            }
                        })
                );
            } else {
                spanBuilder.setNoParent();
            }

            Span serverSpan = spanBuilder.startSpan();
            serverSpan.makeCurrent();
            serverSpan.setAttribute(SemanticAttributes.HTTP_METHOD, method);
            serverSpan.setAttribute(SemanticAttributes.HTTP_SCHEME, requestUri.getScheme());
            serverSpan.setAttribute(SemanticAttributes.HTTP_HOST, requestUri.getHost() + ":" + requestUri.getPort());
            serverSpan.setAttribute(SemanticAttributes.HTTP_TARGET, uri);

            requestContext.setProperty(SERVER_SPAN, serverSpan);
        }
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        Object serverSpan = containerRequestContext.getProperty(SERVER_SPAN);
        if (serverSpan != null && serverSpan instanceof Span) {
            final Span span = (Span) serverSpan;
            final int status = containerResponseContext.getStatus();
            if (status >= 400) {
                span.setAttribute(SemanticAttributes.HTTP_STATUS_CODE, status);
            }

            span.end();
        }
    }
}
