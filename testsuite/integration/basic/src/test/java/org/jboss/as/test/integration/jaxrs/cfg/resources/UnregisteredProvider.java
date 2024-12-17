/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class UnregisteredProvider implements ContainerRequestFilter {
    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        requestContext.abortWith(Response.ok(UnregisteredProvider.class.getName()).build());
    }
}
