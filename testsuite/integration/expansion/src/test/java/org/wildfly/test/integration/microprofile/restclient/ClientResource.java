/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.restclient;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("client")
@RequestScoped
public class ClientResource extends Headers {
    @Inject
    @RestClient
    private TestClient testClient;

    @Inject
    @ConfigProperty(name = "org.eclipse.microprofile.rest.client.propagateHeaders", defaultValue = "n/a")
    private String propagation;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response invokeTestClient() {
        final JsonObjectBuilder builder = Json.createObjectBuilder()
                // Add the config property
                .add("org.eclipse.microprofile.rest.client.propagateHeaders", propagation)
                // Add the incoming headers
                .add("incomingRequestHeaders", generateHeaders());
        try (Response infosResponse = testClient.headers()) {
            builder.add("serverResponse", infosResponse.readEntity(JsonObject.class));
        }
        return Response.ok(builder.build()).build();
    }
}
