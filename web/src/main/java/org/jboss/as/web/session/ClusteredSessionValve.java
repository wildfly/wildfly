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

import static org.jboss.as.web.WebMessages.MESSAGES;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Manager;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.valves.ValveBase;
import org.jboss.as.clustering.web.BatchingManager;
import org.jboss.logging.Logger;
import org.jboss.servlet.http.HttpEvent;

/**
 * This Valve detects all sessions that were used in a request. All sessions are given to a snapshot manager that handles the
 * distribution of modified sessions.
 * <p/>
 * TOMCAT 4.1.12 UPDATE: Added findLifecycleListeners() to comply with the latest Lifecycle interface.
 *
 * @author Thomas Peuss <jboss@peuss.de>
 * @author Brian Stansberry
 * @version $Revision: 87464 $
 */
public class ClusteredSessionValve extends ValveBase implements Lifecycle {

    protected static final Logger log = Logger.getLogger(ClusteredSessionValve.class);

    // The info string for this Valve
    private static final String info = "ClusteredSessionValve/1.0";

    // Valve-lifecycle_ helper object
    private final LifecycleSupport support = new LifecycleSupport(this);

    private final Manager manager;

    private final BatchingManager tm;

    /**
     * Create a new Valve.
     */
    public ClusteredSessionValve(Manager manager, BatchingManager tm) {
        assert manager != null : MESSAGES.nullManager();

        this.manager = manager;
        this.tm = tm;
    }

    /**
     * Get information about this Valve.
     */
    @Override
    public String getInfo() {
        return info;
    }

    /**
     * Valve-chain handler method. This method gets called when the request goes through the Valve-chain. Our session
     * replication mechanism replicates the session after request got through the servlet code.
     *
     * @param request The request object associated with this request.
     * @param response The response object associated with this request.
     */
    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (log.isTraceEnabled()) {
            log.tracef("handling request %s", request.getRequestURI());
        }

        handleRequest(request, response, null, false);
    }

    /**
     * Valve-chain handler method. This method gets called when the request goes through the Valve-chain. Our session
     * replication mechanism replicates the session after request got through the servlet code.
     *
     * @param request The request object associated with this request.
     * @param response The response object associated with this request.
     */
    @Override
    public void event(Request request, Response response, HttpEvent event) throws IOException, ServletException {
        handleRequest(request, response, event, true);
    }

    private void handleRequest(Request request, Response response, HttpEvent event, boolean isEvent) throws IOException,
            ServletException {

        // Initialize the context and store the request and response objects
        // for any clustering code that has no direct access to these objects
        SessionReplicationContext.enterWebapp(request, response, true);

        boolean startedBatch = startBatchTransaction();
        try {
            // Workaround to JBAS-5735. Ensure we get the session from the manager
            // rather than a cached ref from the Request.
            String requestedId = request.getRequestedSessionId();
            if (requestedId != null) {
                manager.findSession(requestedId);
            }

            // let the servlet invocation go through
            if (isEvent) {
                getNext().event(request, response, event);
            } else {
                getNext().invoke(request, response);
            }
        } finally { // We replicate no matter what
            // --> We are now after the servlet invocation
            try {
                SessionReplicationContext ctx = SessionReplicationContext.exitWebapp();

                if (ctx.getSoleSnapshotManager() != null) {
                    ctx.getSoleSnapshotManager().snapshot(ctx.getSoleSession());
                } else {
                    // Cross-context request touched multiple sessions;
                    // need to replicate them all
                    handleCrossContextSessions(ctx);

                }
            } finally {
                if (startedBatch) {
                    tm.endBatch();
                }
            }

        }
    }

    // Lifecycle-interface
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        support.addLifecycleListener(listener);
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        support.removeLifecycleListener(listener);
    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return support.findLifecycleListeners();
    }

    @Override
    public void start() throws LifecycleException {
        support.fireLifecycleEvent(START_EVENT, this);
    }

    @Override
    public void stop() throws LifecycleException {
        support.fireLifecycleEvent(STOP_EVENT, this);
    }

    private boolean startBatchTransaction() throws ServletException {
        boolean started = false;
        try {
            if (this.tm != null && this.tm.isBatchInProgress() == false) {
                this.tm.startBatch();
                started = true;
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new ServletException(MESSAGES.failToStartBatchTransaction(e));
        }

        return started;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void handleCrossContextSessions(SessionReplicationContext ctx) {
        // Genericized code below crashes some Sun JDK compilers; see
        // http://www.jboss.org/index.html?module=bb&op=viewtopic&t=154175

        // Map<ClusteredSession<? extends OutgoingDistributableSessionData>, SnapshotManager> sessions =
        // ctx.getCrossContextSessions();
        // if (sessions != null && sessions.size() > 0)
        // {
        // for (Map.Entry<ClusteredSession<? extends OutgoingDistributableSessionData>, SnapshotManager> entry :
        // sessions.entrySet())
        // {
        // entry.getValue().snapshot(entry.getKey());
        // }
        // }

        // So, use this non-genericized code instead
        Map sessions = ctx.getCrossContextSessions();
        if (sessions != null && sessions.size() > 0) {
            for (Iterator it = sessions.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                ((SnapshotManager) entry.getValue()).snapshot((ClusteredSession) entry.getKey());
            }
        }
    }

}
