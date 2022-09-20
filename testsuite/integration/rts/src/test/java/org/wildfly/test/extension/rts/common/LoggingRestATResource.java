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

import org.jboss.jbossts.star.util.TxLinkNames;
import org.jboss.jbossts.star.util.TxStatus;
import org.jboss.jbossts.star.util.TxSupport;
import org.jboss.logging.Logger;

import jakarta.json.Json;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
@Path(LoggingRestATResource.BASE_URL_SEGMENT)
public class LoggingRestATResource {

    public static final String BASE_URL_SEGMENT = "logging-rest-at-participant";

    public static final String INVOCATIONS_URL_SEGMENT = "invocations";

    private static final Logger LOG = Logger.getLogger(LoggingRestATResource.class);

    private static final List<String> invocations = new ArrayList<String>();

    /**
     * Returns links to the participant terminator.
     *
     * @return Link to the participant terminator.
     */
    @HEAD
    public Response headParticipant(@Context final UriInfo uriInfo) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("LoggingRestATResource.headParticipant()");
        }

        invocations.add("LoggingRestATResource.headParticipant()");

        final String serviceURL = uriInfo.getBaseUri() + uriInfo.getPath();
        final String linkHeader = new TxSupport().makeTwoPhaseAwareParticipantLinkHeader(serviceURL, false, null, null);

        return Response.ok().header("Link", linkHeader).build();
    }

    /**
     * Returns current status of the participant.
     *
     * @return
     */
    @GET
    public Response getStatus() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("LoggingRestATResource.getStatus()");
        }

        invocations.add("LoggingRestATResource.getStatus()");

        return null;
    }

    /**
     * Terminates participant.
     *
     * @param content
     * @return
     */
    @PUT
    @Path(TxLinkNames.TERMINATOR)
    public Response terminateParticipant(String content) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("LoggingRestATResource.terminateParticipant(" + content + ")");
        }

        invocations.add("LoggingRestATResource.terminateParticipant(" + content + ")");

        TxStatus txStatus = TxSupport.toTxStatus(content);
        String responseStatus = null;

        if (txStatus.isPrepare()) {
            responseStatus = TxStatus.TransactionPrepared.name();

        } else if (txStatus.isCommit()) {
            responseStatus = TxStatus.TransactionCommitted.name();

        } else if (txStatus.isCommitOnePhase()) {
            responseStatus = TxStatus.TransactionCommittedOnePhase.name();

        } else if (txStatus.isAbort()) {
            responseStatus = TxStatus.TransactionRolledBack.name();
        }

        if (responseStatus == null) {
            return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).build();
        } else {
            return Response.ok(TxSupport.toStatusContent(responseStatus)).build();
        }
    }

    @GET
    @Path(INVOCATIONS_URL_SEGMENT)
    @Produces(MediaType.APPLICATION_JSON)
    public String getInvocations() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("LoggingRestATResource.getInvocations()");
        }

        return Json.createArrayBuilder(invocations).build().toString();
    }

    @PUT
    @Path(INVOCATIONS_URL_SEGMENT)
    public Response resetInvocations() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("LoggingRestATResource.resetInvocations()");
        }

        invocations.clear();
        return Response.ok().build();
    }

}
