/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.util;

import org.junit.Assert;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Response;

public class ClientConfigProviderBearerTokenAbortFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
        String authorizationHeader = requestContext.getHeaderString("Authorization");
        Assert.assertEquals("The request authorization header is not correct", "Bearer myTestToken", authorizationHeader);
        requestContext.abortWith(Response.ok().build());
    }
}
