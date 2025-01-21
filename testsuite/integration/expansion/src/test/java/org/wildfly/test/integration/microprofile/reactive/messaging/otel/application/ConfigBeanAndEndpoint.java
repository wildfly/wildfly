/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.otel.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@Path("/property")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ConfigBeanAndEndpoint {

    @Inject
    Config config;

    // Just here to pick this up as a RM deployment
    // Not important for this part of the test
    @Inject
    @Channel("invm")
    Emitter<String> emitter;

    @Incoming("invm")
    public void sink(String word) {
    // Just here to pick this up as a RM deployment
    // Not important for this part of the test
    }

    @GET
    public boolean getConfigProperty(@QueryParam("prop") String property) {
        boolean value = config.getValue(property, Boolean.class);
        return value;
    }
}
