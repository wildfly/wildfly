/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.provider;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Path("/")
public class ExceptionResource {

    public static final String PATH_EXCEPTION = "/exception";

    private static final Logger LOGGER = Logger.getLogger(ExceptionResource.class);

    /**
     * Test method for the Provider. Throws an IllegalArgumentException.
     */
    @GET
    @Path(PATH_EXCEPTION)
    public Response testExceptionMapper() {
        LOGGER.trace("Throwing exception");
        throw new IllegalArgumentException("Exception expected.");
    }
}
