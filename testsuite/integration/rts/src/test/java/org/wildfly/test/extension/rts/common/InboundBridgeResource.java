/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.extension.rts.common;

import jakarta.json.Json;
import jakarta.transaction.Transaction;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.arjuna.ats.jta.TransactionManager;
import org.jboss.logging.Logger;


/**
 *
 * @author Gytis Trikleris
 *
 */
@Path(InboundBridgeResource.URL_SEGMENT)
public class InboundBridgeResource {

    public static final String URL_SEGMENT = "inbound-bridge-resource";

    private static final Logger LOG = Logger.getLogger(LoggingRestATResource.class);

    private static LoggingXAResource loggingXAResource;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getInvocations() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("InboundBridgeResource.getInvocations()");
        }

        if (loggingXAResource == null) {
            throw new WebApplicationException(409);
        }

        return Json.createArrayBuilder(loggingXAResource.getInvocations()).build().toString();
    }

    @POST
    @Transactional
    public Response enlistXAResource() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("InboundBridgeResource.enlistXAResource()");
        }

        try {
            loggingXAResource = new LoggingXAResource();

            Transaction t = TransactionManager.transactionManager().getTransaction();
            t.enlistResource(loggingXAResource);

        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);

            return Response.serverError().build();
        }

        return Response.ok().build();
    }

    @PUT
    public Response resetXAResource() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("InboundBridgeResource.resetXAResource()");
        }

        if (loggingXAResource != null) {
            loggingXAResource.resetInvocations();
        }

        return Response.ok().build();
    }

}
