/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.restclient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RegisterRestClient()
@Path("api")
@Produces(MediaType.APPLICATION_JSON)
@RegisterClientHeaders()
public interface TestClient {
    @GET
    @Path("headers")
    @ClientHeaderParam(name = "TestClientHeader", value = "client-value")
    Response headers();
}
