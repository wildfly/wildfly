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
package org.jboss.as.undertow.session;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletAttachments;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import org.jboss.as.clustering.web.BatchingManager;
import org.jboss.as.undertow.UndertowMessages;
import org.jboss.logging.Logger;


/**
 * This handler detects all sessions that were used in a request. All sessions are given to a snapshot manager that handles the
 * distribution of modified sessions.
 *
 * @author Thomas Peuss <jboss@peuss.de>
 * @author Brian Stansberry
 * @version $Revision: 87464 $
 */
public class ClusteredSessionHandler implements HttpHandler {

    protected static final Logger log = Logger.getLogger(ClusteredSessionHandler.class);

    private final SessionManager manager;

    private final BatchingManager tm;

    private final HttpHandler next;

    /**
     * Create a new Valve.
     */
    public ClusteredSessionHandler(final SessionManager manager, BatchingManager tm, final HttpHandler next) {
        this.next = next;
        assert manager != null : UndertowMessages.MESSAGES.nullParamter("manager");

        this.manager = manager;
        this.tm = tm;
    }


    /**
     * Our session
     * replication mechanism replicates the session after request got through the servlet code.
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (log.isTraceEnabled()) {
            log.tracef("handling request %s", exchange.getRequestURI());
        }

        // Initialize the context and store the request and response objects
        // for any clustering code that has no direct access to these objects
        SessionReplicationContext.enterWebapp(exchange, true);

        boolean startedBatch = startBatchTransaction();
        try {
            // Workaround to JBAS-5735. Ensure we get the session from the manager
            // rather than a cached ref from the Request.
            final HttpServletRequestImpl request = HttpServletRequestImpl.getRequestImpl(exchange.getAttachment(ServletAttachments.ATTACHMENT_KEY).getServletRequest());

            String requestedId = request.getServletContext().getSessionCookieConfig().findSessionId(exchange);
            if (requestedId != null) {
                manager.getSession(exchange, request.getServletContext().getSessionCookieConfig());
            }

            // let the servlet invocation go through
            next.handleRequest(exchange);
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
            throw UndertowMessages.MESSAGES.failToStartBatchTransaction(e);
        }

        return started;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
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
            for (Iterator it = sessions.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                ((SnapshotManager) entry.getValue()).snapshot((ClusteredSession) entry.getKey());
            }
        }
    }
}
