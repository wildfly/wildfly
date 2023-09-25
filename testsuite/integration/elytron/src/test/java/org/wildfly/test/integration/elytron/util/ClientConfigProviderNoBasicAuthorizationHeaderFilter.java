/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.util;

import org.junit.Assert;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

public class ClientConfigProviderNoBasicAuthorizationHeaderFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
        String authorizationHeader = requestContext.getHeaderString("Authorization");
        Assert.assertTrue("There should be no Basic authorization header", authorizationHeader == null || !authorizationHeader.contains("Basic"));
    }
}
