/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.provider;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/**
 * A Jakarta RESTful Web Services test implementation for {@link ExceptionMapper}. If an Exception occurs, returns HTTP OK (200) and prints
 * {@value #ERROR_MESSAGE} as the response body.
 *
 * @author Josef Cacek
 */
@Provider
@Path("/")
public class ExceptionMapperProvider implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = Logger.getLogger(ExceptionMapperProvider.class);

    public static final String ERROR_MESSAGE = "ERROR OCCURRED";
    public static final String PATH_EXCEPTION = "/exception";

    // Public methods --------------------------------------------------------

    /**
     * Responds {@value #ERROR_MESSAGE} to the OK (200) response.
     *
     * @param exception
     *
     * @return
     *
     * @see jakarta.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
     */
    @Override
    public Response toResponse(Exception exception) {
        LOGGER.trace("Mapped exception", exception);
        return Response.ok().entity(ERROR_MESSAGE).build();
    }

    /**
     * Test method for the Provider. Throws an IllegalArgumentException.
     *
     * @return
     */
    @GET
    @Path(PATH_EXCEPTION)
    public Response testExceptionMapper() {
        LOGGER.trace("Throwing exception");
        throw new IllegalArgumentException("Exception expected.");
    }

}
