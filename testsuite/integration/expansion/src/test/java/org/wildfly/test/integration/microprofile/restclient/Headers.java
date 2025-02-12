/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.restclient;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class Headers {
    @Inject
    private HttpHeaders headers;

    /**
     * Creates a new JSON object based on the HTTP request headers. The name of the header is the key and the value is
     * an array of the header values.
     *
     * @return an object of the request headers
     */
    protected JsonObjectBuilder generateHeaders() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        headers.getRequestHeaders().forEach((name, value) -> builder.add(name, Json.createArrayBuilder(value)));
        return builder;
    }
}
