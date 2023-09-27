/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.exception;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Exception mapper - occurred NPE exception remaps to HTTP 404 Error code
 *
 * @author Pavel Janousek
 */
@Provider
public class NPExceptionMapper implements ExceptionMapper<NullPointerException> {
    public Response toResponse(NullPointerException ex) {
        return Response.status(HttpServletResponse.SC_NOT_FOUND).build();
    }
}
