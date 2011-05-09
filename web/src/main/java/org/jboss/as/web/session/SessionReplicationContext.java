/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.session;

import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;

public final class SessionReplicationContext {
    private static final ThreadLocal<SessionReplicationContext> replicationContext = new ThreadLocal<SessionReplicationContext>();

    private static final SessionReplicationContext EMPTY = new SessionReplicationContext();

    private int webappCount;
    // private int activityCount;
    private SnapshotManager soleManager;
    private ClusteredSession<? extends OutgoingDistributableSessionData> soleSession;
    private Map<ClusteredSession<? extends OutgoingDistributableSessionData>, SnapshotManager> crossCtxSessions;
    private Request outerRequest;
    private Response outerResponse;

    /**
     * Associate a SessionReplicationContext with the current thread, if there isn't one already. If there isn't one, associate
     * the given request and response with the context.
     * <p/>
     * <strong>NOTE:</strong> Nested calls to this method and {@link #exitWebapp()} are supported; once a context is established
     * the number of calls to this method and <code>exitWebapp()</code> are tracked.
     *
     * @param request
     * @param response
     */
    public static void enterWebapp(Request request, Response response, boolean startCacheActivity) {
        SessionReplicationContext ctx = getCurrentContext();
        if (ctx == null) {
            ctx = new SessionReplicationContext(request, response);
            replicationContext.set(ctx);
        }

        ctx.webappCount++;
    }

    /**
     * Signals that the webapp is finished handling the request (and therefore replication can begin.)
     *
     * @return a SessionReplicationContext, from which information about any sessions needing replication can be obtained. Will
     *         not return <code>null</code>.
     */
    public static SessionReplicationContext exitWebapp() {
        SessionReplicationContext ctx = getCurrentContext();
        if (ctx != null) {
            ctx.webappCount--;
            if (ctx.webappCount < 1) {
                // We've unwound any nested webapp calls, so we'll clean up and
                // return the context to allow replication. If all cache activity
                // is done as well, clear the ThreadLocal

                ctx.outerRequest = null;
                ctx.outerResponse = null;

                replicationContext.set(null);

                return ctx;
            }
        }

        // A nested valve called us. Just return an empty context
        return EMPTY;

    }

    public static void bindSession(ClusteredSession<? extends OutgoingDistributableSessionData> session, SnapshotManager manager) {
        SessionReplicationContext ctx = getCurrentContext();
        if (ctx != null && ctx.webappCount > 0) {
            ctx.addReplicatableSession(session, manager);
        }
        /*
         * else { We are past the part of the request cycle where we track sessions for replication }
         */
    }

    public static void sessionExpired(ClusteredSession<? extends OutgoingDistributableSessionData> session, String realId, SnapshotManager manager) {
        SessionReplicationContext ctx = getCurrentContext();
        if (ctx != null && ctx.webappCount > 0) {
            ctx.sessionExpired(session, manager);
        }
    }

    /**
     * Returns whether there is a SessionReplicationContext associated with the current thread.
     *
     * @return <code>true</code> if there is a context associated with the thread
     */
    public static boolean isLocallyActive() {
        return getCurrentContext() != null;
    }

    public static Request getOriginalRequest() {
        SessionReplicationContext ctx = getCurrentContext();
        return (ctx == null ? null : ctx.outerRequest);
    }

    public static Response getOriginalResponse() {
        SessionReplicationContext ctx = getCurrentContext();
        return (ctx == null ? null : ctx.outerResponse);
    }

    private static SessionReplicationContext getCurrentContext() {
        return replicationContext.get();
    }

    private SessionReplicationContext(Request request, Response response) {
        this.outerRequest = request;
        this.outerResponse = response;
    }

    private SessionReplicationContext() {
    }

    /**
     * Gets a Map<SnapshotManager, ClusteredSession> of sessions that were accessed during the course of a request. Will only be
     * non-null if {@link #bindSession(ClusteredSession, SnapshotManager)} was called with more than one SnapshotManager (i.e
     * the request crossed session contexts.)
     */
    public Map<ClusteredSession<? extends OutgoingDistributableSessionData>, SnapshotManager> getCrossContextSessions() {
        return crossCtxSessions;
    }

    /**
     * Gets the SnapshotManager that was passed to {@link #bindSession(ClusteredSession, SnapshotManager)} if and only if only
     * one such SnapshotManager was passed. Returns <code>null</code> otherwise, in which case a cross-context request is a
     * possibility, and {@link #getCrossContextSessions()} should be checked.
     */
    public SnapshotManager getSoleSnapshotManager() {
        return soleManager;
    }

    /**
     * Gets the ClusteredSession that was passed to {@link #bindSession(ClusteredSession, SnapshotManager)} if and only if only
     * one SnapshotManager was passed. Returns <code>null</code> otherwise, in which case a cross-context request is a
     * possibility, and {@link #getCrossContextSessions()} should be checked.
     */
    public ClusteredSession<? extends OutgoingDistributableSessionData> getSoleSession() {
        return soleSession;
    }

    private void addReplicatableSession(ClusteredSession<? extends OutgoingDistributableSessionData> session, SnapshotManager mgr) {
        if (crossCtxSessions != null) {
            crossCtxSessions.put(session, mgr);
        } else if (soleManager == null) {
            // First one bound
            soleManager = mgr;
            soleSession = session;
        } else if (!mgr.equals(soleManager)) {
            // We have a cross-context call; need a Map for the sessions
            crossCtxSessions = new HashMap<ClusteredSession<? extends OutgoingDistributableSessionData>, SnapshotManager>();
            crossCtxSessions.put(soleSession, soleManager);
            crossCtxSessions.put(session, mgr);
            soleManager = null;
            soleSession = null;
        } else {
            soleSession = session;
        }
    }

    private void sessionExpired(ClusteredSession<? extends OutgoingDistributableSessionData> session, SnapshotManager manager) {
        if (manager.equals(soleManager)) {
            soleManager = null;
            soleSession = null;
        } else if (crossCtxSessions != null) {
            crossCtxSessions.remove(session);
        }
    }
}
