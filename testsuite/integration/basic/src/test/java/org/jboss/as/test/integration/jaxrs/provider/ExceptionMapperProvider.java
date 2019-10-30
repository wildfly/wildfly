/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.jaxrs.provider;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

/**
 * A JAX-RS test implementation for {@link ExceptionMapper}. If an Exception occurs, returns HTTP OK (200) and prints
 * {@value #ERROR_MESSAGE} as the response body.
 *
 * @author Josef Cacek
 */
@Provider
@Startup
@Singleton
@Path("/")
public class ExceptionMapperProvider implements ExceptionMapper<Exception> {

    private static Logger LOGGER = Logger.getLogger(ExceptionMapperProvider.class);

    public static final String ERROR_MESSAGE = "ERROR OCCURRED";
    public static final String PATH_EXCEPTION = "/exception";

    // Public methods --------------------------------------------------------

    /**
     * Responds {@value #ERROR_MESSAGE} to the OK (200) response.
     *
     * @param exception
     * @return
     * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
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
