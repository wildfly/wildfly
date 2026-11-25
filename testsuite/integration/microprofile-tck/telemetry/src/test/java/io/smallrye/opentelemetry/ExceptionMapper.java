/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.smallrye.opentelemetry;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

/**
 * Temporary fix to catch exceptions thrown in Jakarta RESTful Web Services endpoints, see https://issues.redhat.com/browse/RESTEASY-1758
 *
 * @author Pavol Loffay
 */
@Provider
public class ExceptionMapper implements jakarta.ws.rs.ext.ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
}
