/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.extension.rts.common;

import org.jboss.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path(InboundBridgeResourceEjb.URL_SEGMENT)
@Stateless
public class InboundBridgeResourceEjb {
    private static final Logger LOG = Logger.getLogger(LoggingRestATResource.class);
    public static final String URL_SEGMENT = "inbound-bridge-resource-ejb";

    private static LoggingXAResource loggingXAResource;

    @Resource(lookup = "java:/TransactionManager")
    jakarta.transaction.TransactionManager transactionManager;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getInvocations() {
        LOG.tracef("getInvocations() for resource '%s'", loggingXAResource);

        if (loggingXAResource == null) {
            throw new WebApplicationException(409);
        }
        return Json.createArrayBuilder(loggingXAResource.getInvocations()).build().toString();
    }

    @POST
    public Response enlistXAResource() {
        LOG.tracef("enlistXAResource() of resource '%s'", loggingXAResource);

        try {
            loggingXAResource = new LoggingXAResource();
            transactionManager.getTransaction().enlistResource(loggingXAResource);
        } catch (Exception e) {
            LOG.warnf(e, "Cannot enlist XAResource '%s'", loggingXAResource);
            return Response.serverError().build();
        }
        return Response.ok().build();
    }

    @PUT
    public Response resetXAResource() {
        LOG.tracef("resetXAResource() of resource", loggingXAResource);

        if (loggingXAResource != null) {
            loggingXAResource.resetInvocations();
        }

        return Response.ok().build();
    }
}
