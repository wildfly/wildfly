/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Path("/wider")
public class WiderMappingResource {
    @POST
    @Path("string")
    public String post(final String echo) {
        return echo;
    }
}
