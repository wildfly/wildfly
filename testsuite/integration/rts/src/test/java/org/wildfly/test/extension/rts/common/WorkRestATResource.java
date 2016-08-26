/*
* JBoss, Home of Professional Open Source.
* Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.jbossts.star.provider.HttpResponseException;
import org.jboss.jbossts.star.util.TxLinkNames;
import org.jboss.jbossts.star.util.TxMediaType;
import org.jboss.jbossts.star.util.TxStatus;
import org.jboss.jbossts.star.util.TxStatusMediaType;
import org.jboss.jbossts.star.util.TxSupport;

@Path(WorkRestATResource.PATH_SEGMENT)
public final class WorkRestATResource {

    public static final String PATH_SEGMENT = "txresource";

    private static int pid = 0;

    private static Map<String, Work> faults = new HashMap<String, Work>();

    public static void clearFaults() {
        faults.clear();
    }

    @GET
    public String getBasic(@Context UriInfo info, @QueryParam("pId") @DefaultValue("") String pId,
            @QueryParam("context") @DefaultValue("") String ctx, @QueryParam("name") @DefaultValue("") String name,
            @QueryParam("value") @DefaultValue("") String value, @QueryParam("query") @DefaultValue("pUrl") String query,
            @QueryParam("arg") @DefaultValue("") String arg,
            @QueryParam("twoPhaseAware") @DefaultValue("true") String twoPhaseAware,
            @QueryParam("isVolatile") @DefaultValue("false") String isVolatileParticipant,
            @QueryParam("register") @DefaultValue("true") String register) {

        Work work = faults.get(pId);
        String res = null;
        boolean isVolatile = "true".equals(isVolatileParticipant);
        boolean isTwoPhaseAware = "true".equals(twoPhaseAware);

        if (name.length() != 0) {
            if (value.length() != 0) {
                if (work == null) {
                    work = makeWork(new TxSupport(), info.getAbsolutePath().toString(), String.valueOf(++pid), null, null,
                            isTwoPhaseAware, isVolatile, null, null);
                    work.oldState.put(name, value);
                    faults.put(work.id, work);
                    return work.id;
                }

                work.newState.put(name, value);
            }

            if (work != null) {
                if ("syncCount".equals(name))
                    res = String.valueOf(work.syncCount);
                else if ("commitCnt".equals(name))
                    res = String.valueOf(work.commitCnt);
                else if ("prepareCnt".equals(name))
                    res = String.valueOf(work.prepareCnt);
                else if ("rollbackCnt".equals(name))
                    res = String.valueOf(work.rollbackCnt);
                else if ("commmitOnePhaseCnt".equals(name))
                    res = String.valueOf(work.commmitOnePhaseCnt);
                else if (work.inTxn())
                    res = work.newState.get(name);
                else
                    res = work.oldState.get(name);
            }
        }

        if (work == null)
            throw new WebApplicationException(HttpURLConnection.HTTP_NOT_FOUND);

        if ("move".equals(query)) {
            /* Ignore*/
        }else if ("recoveryUrl".equals(query))
            res = work.recoveryUrl;
        else if ("status".equals(query))
            res = work.status;
        else if (res == null)
            res = work.pLinks;

        return res; // null will generate a 204 status code (no content)
    }

    @POST
    @Produces(TxMediaType.PLAIN_MEDIA_TYPE)
    public String enlist(@Context UriInfo info, @QueryParam("pId") @DefaultValue("") String pId,
            @QueryParam("fault") @DefaultValue("") String fault,
            @QueryParam("twoPhaseAware") @DefaultValue("true") String twoPhaseAware,
            @QueryParam("isVolatile") @DefaultValue("false") String isVolatile, String enlistUrl) throws IOException {

        Work work = faults.get(pId);
        TxSupport txn = new TxSupport();
        String txId = enlistUrl.substring(enlistUrl.lastIndexOf('/') + 1);
        boolean isTwoPhaseAware = "true".equals(twoPhaseAware);
        boolean isVolatileParticipant = "true".equals(isVolatile);
        String vRegistration = null; // URI for registering with the volatile phase
        String vParticipantLink = null; // URI for handling pre and post 2PC phases

        if (work == null) {
            int id = ++pid;

            work = makeWork(txn, info.getAbsolutePath().toString(), String.valueOf(id), txId, enlistUrl, isTwoPhaseAware,
                    isVolatileParticipant, null, fault);
        } else {
            Work newWork = makeWork(txn, info.getAbsolutePath().toString(), work.id, txId, enlistUrl, isTwoPhaseAware,
                    isVolatileParticipant, null, fault);
            newWork.oldState = work.oldState;
            newWork.newState = work.newState;
            work = newWork;
        }

        if (enlistUrl.indexOf(',') != -1) {
            String[] urls = enlistUrl.split(",");

            if (urls.length < 2)
                throw new WebApplicationException(HttpURLConnection.HTTP_BAD_REQUEST);

            enlistUrl = urls[0];
            vRegistration = urls[1];

            String vParticipant = new StringBuilder(info.getAbsolutePath().toString()).append('/').append(work.id).append('/')
                    .append(txId).append('/').append("vp").toString();
            vParticipantLink = txn.addLink2(new StringBuilder(), TxLinkNames.VOLATILE_PARTICIPANT, vParticipant, true)
                    .toString();
        }

        try {
            // enlist TestResource in the transaction as a participant
            work.recoveryUrl = txn.enlistParticipant(enlistUrl, work.pLinks);

            if (vParticipantLink != null)
                txn.enlistVolatileParticipant(vRegistration, vParticipantLink);
        } catch (HttpResponseException e) {
            throw new WebApplicationException(e.getActualResponse());
        }

        work.status = TxStatus.TransactionActive.name();
        work.start();

        faults.put(work.id, work);

        return work.id;
    }

    @PUT
    @Path("{pId}/{tId}/vp")
    public Response directSynchronizations(@PathParam("pId") @DefaultValue("") String pId,
            @PathParam("tId") @DefaultValue("") String tId, String content) {

        return synchronizations(pId, tId, content);
    }

    @PUT
    @Path("{pId}/{tId}/volatile-participant")
    public Response synchronizations(@PathParam("pId") @DefaultValue("") String pId,
            @PathParam("tId") @DefaultValue("") String tId, String content) {

        Work work = faults.get(pId);
        TxStatus txStatus;
        int vStatus;

        if (work == null)
            return Response.ok().build();

        txStatus = content != null ? TxStatus.fromStatus(content) : TxStatus.TransactionStatusUnknown;

        vStatus = txStatus.equals(TxStatus.TransactionStatusUnknown) ? 1 : 2;

        if (vStatus == 2 && work.vStatus == 0) {
            // afterCompletion but coordinator never called beforeCompletion
            return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).build();
        }

        work.vStatus = vStatus;
        work.syncCount += 1;

        if (vStatus == 1 && "V_PREPARE".equals(work.fault))
            return Response.status(HttpURLConnection.HTTP_CONFLICT).build();
        else if (vStatus == 2 && "V_COMMIT".equals(work.fault))
            return Response.status(HttpURLConnection.HTTP_CONFLICT).build();

        return Response.ok().build();
    }

    @PUT
    @Path("{pId}/{tId}/terminator")
    public Response terminate(@PathParam("pId") @DefaultValue("") String pId, @PathParam("tId") @DefaultValue("") String tId,
            String content) {

        TxStatus status = TxSupport.toTxStatus(content);

        // String status = TxSupport.getStatus(content);
        Work work = faults.get(pId);

        if (work == null)
            return Response.status(HttpURLConnection.HTTP_NOT_FOUND).build();

        String fault = work.fault;

        if (status.isPrepare()) {
            if ("READONLY".equals(fault)) {
                // faults.remove(pId);
                work.status = TxStatus.TransactionReadOnly.name();
            } else if ("PREPARE_FAIL".equals(fault)) {
                // faults.remove(pId);
                return Response.status(HttpURLConnection.HTTP_CONFLICT).build();
                // throw new WebApplicationException(HttpURLConnection.HTTP_CONFLICT);
            } else {
                if ("PDELAY".equals(fault)) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                }
                work.status = TxStatus.TransactionPrepared.name();
            }
        } else if (status.isCommit() || status.isCommitOnePhase()) {
            if ("H_HAZARD".equals(fault))
                work.status = TxStatus.TransactionHeuristicHazard.name();
            else if ("H_ROLLBACK".equals(fault))
                work.status = TxStatus.TransactionHeuristicRollback.name();
            else if ("H_MIXED".equals(fault))
                work.status = TxStatus.TransactionHeuristicMixed.name();
            else {
                if ("CDELAY".equals(fault)) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        // ok
                    }
                }
                work.status = status.isCommitOnePhase() ? TxStatus.TransactionCommittedOnePhase.name()
                        : TxStatus.TransactionCommitted.name();

                work.end(true);
            }
        } else if (status.isAbort()) {
            if ("H_HAZARD".equals(fault))
                work.status = TxStatus.TransactionHeuristicHazard.name();
            else if ("H_COMMIT".equals(fault))
                work.status = TxStatus.TransactionHeuristicCommit.name();
            else if ("H_MIXED".equals(fault))
                work.status = TxStatus.TransactionHeuristicMixed.name();
            else {
                if ("ADELAY".equals(fault)) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        // ok
                    }
                }
                work.status = TxStatus.TransactionRolledBack.name();
                work.end(false);
                // faults.remove(pId);
            }
        } else {
            return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).build();
            // throw new WebApplicationException(HttpURLConnection.HTTP_BAD_REQUEST);
        }

        // return TxSupport.toStatusContent(work.status);
        return Response.ok(TxSupport.toStatusContent(work.status)).build();
    }

    @PUT
    @Path("{pId}/{tId}/prepare")
    public Response prepare(@PathParam("pId") @DefaultValue("") String pId, @PathParam("tId") @DefaultValue("") String tId,
            String content) {

        Work work = faults.get(pId);
        if (work != null)
            work.prepareCnt += 1;
        return terminate(pId, tId, TxStatusMediaType.TX_PREPARED);
    }

    @PUT
    @Path("{pId}/{tId}/commit")
    public Response commit(@PathParam("pId") @DefaultValue("") String pId, @PathParam("tId") @DefaultValue("") String tId,
            String content) {

        Work work = faults.get(pId);
        if (work != null)
            work.commitCnt += 1;
        return terminate(pId, tId, TxStatusMediaType.TX_COMMITTED);
    }

    @PUT
    @Path("{pId}/{tId}/rollback")
    public Response rollback(@PathParam("pId") @DefaultValue("") String pId, @PathParam("tId") @DefaultValue("") String tId,
            String content) {

        Work work = faults.get(pId);
        if (work != null)
            work.rollbackCnt += 1;
        return terminate(pId, tId, TxStatusMediaType.TX_ROLLEDBACK);
    }

    @PUT
    @Path("{pId}/{tId}/commit-one-phase")
    public Response commmitOnePhase(@PathParam("pId") @DefaultValue("") String pId,
            @PathParam("tId") @DefaultValue("") String tId, String content) {

        Work work = faults.get(pId);
        if (work != null)
            work.commmitOnePhaseCnt += 1;
        return terminate(pId, tId, TxStatusMediaType.TX_COMMITTED_ONE_PHASE);
    }

    @HEAD
    @Path("{pId}/{tId}/participant")
    public Response getTerminator(@Context UriInfo info, @PathParam("pId") @DefaultValue("") String pId,
            @PathParam("tId") @DefaultValue("") String tId) {

        Work work = faults.get(pId);

        if (work == null)
            return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).build();

        Response.ResponseBuilder builder = Response.ok();

        builder.header("Link", work.pLinks);

        return builder.build();
    }

    @GET
    @Path("{pId}/{tId}/participant")
    public String getStatus(@PathParam("pId") @DefaultValue("") String pId, @PathParam("tId") @DefaultValue("") String tId) {
        Work work = faults.get(pId);

        if (work == null)
            throw new WebApplicationException(HttpURLConnection.HTTP_NOT_FOUND);

        return TxSupport.toStatusContent(work.status);

    }

    @DELETE
    @Path("{pId}/{tId}/participant")
    public void forgetWork(@PathParam("pId") String pId, @PathParam("tId") String tId) {
        Work work = faults.get(pId);

        if (work == null)
            throw new WebApplicationException(HttpURLConnection.HTTP_NOT_FOUND);

        faults.remove(pId);

    }

    public Work makeWork(TxSupport txn, String baseURI, String id, String txId, String enlistUrl, boolean twoPhaseAware,
            boolean isVolatile, String recoveryUrl, String fault) {

        String linkHeader = twoPhaseAware ? txn.makeTwoPhaseAwareParticipantLinkHeader(baseURI, isVolatile, id, txId) : txn
                .makeTwoPhaseUnAwareParticipantLinkHeader(baseURI, isVolatile, id, txId, true);

        return new Work(id, txId, baseURI + '/' + id, linkHeader, enlistUrl, recoveryUrl, fault);
    }

}
