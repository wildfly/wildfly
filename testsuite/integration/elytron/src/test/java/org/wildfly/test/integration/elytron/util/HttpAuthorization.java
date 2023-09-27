/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class HttpAuthorization {

    public static ClientRequestFilter basic(final String username, final String password) {
        return requestContext -> {
            final String key = username + ':' + password;
            final String authHeader = "Basic " + Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
            requestContext.getHeaders().putSingle(HttpHeaders.AUTHORIZATION, authHeader);
        };
    }
}
