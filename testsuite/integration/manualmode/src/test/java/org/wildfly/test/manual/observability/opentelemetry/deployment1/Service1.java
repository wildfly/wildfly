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

package org.wildfly.test.manual.observability.opentelemetry.deployment1;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.wildfly.test.manual.observability.opentelemetry.ContextPropagationTestCase;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class Service1 {
    @Context
    private UriInfo uriInfo;
    @Inject
    private Tracer tracer;

    @GET
    public String endpoint1() throws JsonProcessingException {
        final Span span = tracer.spanBuilder("Doing some work").startSpan();
        span.makeCurrent();

        String traceParent = sendRequest();
        span.end();

        Map<String, String> values = new HashMap<>();
        values.put("traceId", span.getSpanContext().getTraceId());
        values.put("traceParent", traceParent);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(values);
    }

    private String sendRequest() {
        String endpoint2 = uriInfo.getRequestUri().toString()
                .replace(ContextPropagationTestCase.DEPLOYMENTA, ContextPropagationTestCase.DEPLOYMENTB);

        Client client = ClientBuilder.newClient();
        final Response response = client
                .target(endpoint2)
                .request(MediaType.TEXT_PLAIN)
                .get();
        if (response.getStatus() != 200) {
            throw new WebApplicationException(response.getStatus());
        }
        String body = response.readEntity(String.class);
        client.close();

        return body;
    }
}
