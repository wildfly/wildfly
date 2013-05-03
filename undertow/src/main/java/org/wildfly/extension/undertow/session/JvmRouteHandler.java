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
package org.wildfly.extension.undertow.session;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletAttachments;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.logging.Logger;

/**
 * Web request handler to specifically handle Tomcat jvmRoute using mod_jk(2) module. We assume that the session id has a format
 * of id.jvmRoute where jvmRoute is used by JK module to determine sticky session during load balancing. Checks for failover by
 * matching session and request jvmRoute to the session manager's, updating the session and session cookie if a failover is
 * detected.
 * <p/>
 * This handler is inserted in the pipeline only when mod_jk is configured.
 *
 * @author Ben Wang
 * @author Brian Stansberry
 * @version $Revision: 108925 $
 */
public class JvmRouteHandler implements HttpHandler {
    // The info string for this Valve
    private static final String info = "JvmRouteValve/1.0";

    private static final Logger log = Logger.getLogger(JvmRouteHandler.class);

    private final SessionManager manager;

    private final HttpHandler next;

    /**
     * Create a new handler.
     */
    public JvmRouteHandler(SessionManager manager, final HttpHandler next) {
        super();
        this.manager = manager;
        this.next = next;
    }

    public String getInfo() {
        return info;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        if (log.isTraceEnabled()) {
            log.tracef("handling request %s", exchange.getRequestURI());
        }

        // Need to check it before let request through.
        checkJvmRoute(exchange);

        // let the servlet invocation go through
        next.handleRequest(exchange);
    }

    public void checkJvmRoute(final HttpServerExchange exchange) throws IOException, ServletException {
        final ServletAttachments servletAttachments = exchange.getAttachment(ServletAttachments.ATTACHMENT_KEY);
        final HttpServletRequestImpl request = HttpServletRequestImpl.getRequestImpl(servletAttachments.getServletRequest());
        String requestedId = request.getServletContext().getSessionCookieConfig().findSessionId(exchange);
        HttpServletRequestImpl req = (HttpServletRequestImpl) servletAttachments.getServletRequest();
        HttpSession session = req.getSession(false);
        if (session != null) {
            String sessionId = session.getId();
            String jvmRoute = manager.getJvmRoute();

            if (log.isTraceEnabled()) {
                log.tracef("checkJvmRoute(): check if need to re-route based on JvmRoute. Session id: %s jvmRoute: %s", sessionId, jvmRoute);
            }

            if (jvmRoute != null) {
                // Check if incoming session id has JvmRoute appended. If not, append it.
                boolean setCookie = !req.isRequestedSessionIdFromURL();
                handleJvmRoute(requestedId, sessionId, jvmRoute, exchange, setCookie);
            }
        }
    }

    protected void handleJvmRoute(String requestedId, String sessionId, String jvmRoute, HttpServerExchange exchange, boolean setCookie) throws IOException {
        // The new id we'll give the session if we detect a failover
        String newId = null;

        // First, check if the session object's jvmRoute matches ours

        // TODO. The current format is assumed to be id.jvmRoute. Can be generalized later.
        Map.Entry<String, String> sessionEntry = this.manager.parse(sessionId);
        String realId = sessionEntry.getKey();
        String sessionJvmRoute = sessionEntry.getValue();

        if (sessionJvmRoute == null) {
            newId = this.manager.createSessionId(realId, jvmRoute);
        } else if (!jvmRoute.equals(sessionJvmRoute)) {
            if (log.isTraceEnabled()) {
                log.tracef("handleJvmRoute(): We have detected a failover with different jvmRoute. old one: %s, new one: %s. Will reset the session id.", sessionJvmRoute, jvmRoute);
            }
            newId = this.manager.createSessionId(realId, this.manager.locateJvmRoute(realId));
        }

        if (newId != null) {
            // Fix the session's id
            resetSessionId(exchange, sessionId, newId);
        }

        // Now we know the session object has a correct id
        // Also need to ensure any session cookie is correct
        if (setCookie) {
            // Check if the jvmRoute of the requested session id matches ours.
            // Only bother if we haven't already spotted a mismatch above
            if (newId == null) {
                String requestedJvmRoute = (requestedId != null) ? this.manager.parse(requestedId).getValue() : null;

                if (!jvmRoute.equals(requestedJvmRoute)) {
                    if (log.isTraceEnabled()) {
                        log.tracef("handleJvmRoute(): We have detected a failover with different jvmRoute. received one: %s, new one: %s. Will reset the session id.", requestedJvmRoute, jvmRoute);
                    }
                    newId = this.manager.createSessionId(realId, this.manager.locateJvmRoute(realId));
                }
            }

            /* Change the sessionid cookie if needed */
            if (newId != null) {
                final HttpServletRequestImpl request = HttpServletRequestImpl.getRequestImpl(exchange.getAttachment(ServletAttachments.ATTACHMENT_KEY).getServletRequest());
                request.getServletContext().getSessionCookieConfig().setSessionId(exchange, newId);
            }
        }
    }

    /**
     * Update the id of the given session
     *
     * @param oldId id of the session to change
     * @param newId new session id the session object should have
     */
    private void resetSessionId(HttpServerExchange exchange, String oldId, String newId) throws IOException {
        final HttpServletRequestImpl request = HttpServletRequestImpl.getRequestImpl(exchange.getAttachment(ServletAttachments.ATTACHMENT_KEY).getServletRequest());
        ClusteredSession<? extends OutgoingDistributableSessionData> session = (ClusteredSession<?>) manager.getSession(exchange, request.getServletContext().getSessionCookieConfig());
        // change session id with the new one using local jvmRoute.
        if (session != null) {
            session.resetIdWithRouteInfo(newId);

            if (log.isTraceEnabled()) {
                log.tracef("resetSessionId(): changed catalina session to= [%s] old one= [%s]", newId, oldId);
            }
        } else if (log.isTraceEnabled()) {
            log.tracef("resetSessionId(): no session with id %s found", newId);
        }
    }

}
