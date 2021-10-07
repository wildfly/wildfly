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

package org.wildfly.test.manual.observability.opentelemetry.deployment2;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

@Path("/")
public class Service2 {
    private static final String TRACE_PARENT = "traceparent";
    @Context
    private HttpHeaders headers;

    @Inject
    private Tracer tracer;

    @GET
    public String endpoint1() {
        final Span span = tracer.spanBuilder("Doing some work in service2").startSpan();
        String traceParent = headers.getHeaderString(TRACE_PARENT);
        if (traceParent == null || traceParent.isEmpty()) {
            throw new WebApplicationException("Missing traceparent header");
        }
        span.end();
        return traceParent;
    }
}
