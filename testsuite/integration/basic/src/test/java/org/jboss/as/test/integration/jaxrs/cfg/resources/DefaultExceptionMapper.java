/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * This overrides the default exception provided by RESTEasy. In
 * <a href="https://redhat.atlassian.net/browse/RESTEASY-3823">RESTEASY-3823</a> the default exception mapper was
 * changed to no longer display the message from the exception. This was considered an information discloser. However,
 * there are cases for testing purposes where we do want to see these messages.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(final Throwable exception) {
        // We must return the response from a WebApplicationException.
        if (exception instanceof WebApplicationException) {
            return ((WebApplicationException) exception).getResponse();
        }
        return Response.serverError()
                .entity(exception.getLocalizedMessage())
                .build();
    }
}
