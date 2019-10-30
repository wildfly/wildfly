/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.extension.rts.common;

import javax.transaction.Transaction;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONArray;

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

        return new JSONArray(loggingXAResource.getInvocations()).toString();
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
