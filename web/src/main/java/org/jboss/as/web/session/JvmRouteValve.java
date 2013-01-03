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

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.valves.ValveBase;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.logging.Logger;

/**
 * Web request valve to specifically handle Tomcat jvmRoute using mod_jk(2) module. We assume that the session id has a format
 * of id.jvmRoute where jvmRoute is used by JK module to determine sticky session during load balancing. Checks for failover by
 * matching session and request jvmRoute to the session manager's, updating the session and session cookie if a failover is
 * detected.
 *
 * This valve is inserted in the pipeline only when mod_jk is configured.
 *
 * @author Ben Wang
 * @author Brian Stansberry
 *
 * @version $Revision: 108925 $
 */
public class JvmRouteValve extends ValveBase implements Lifecycle {
    // The info string for this Valve
    private static final String info = "JvmRouteValve/1.0";

    protected static Logger log_ = Logger.getLogger(JvmRouteValve.class);

    // Valve-lifecycle_ helper object
    protected LifecycleSupport support = new LifecycleSupport(this);

    protected SessionManager manager;

    /**
     * Create a new Valve.
     *
     */
    public JvmRouteValve(SessionManager manager) {
        super();
        this.manager = manager;
    }

    /**
     * Get information about this Valve.
     */
    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (log_.isTraceEnabled()) {
            log_.tracef("handling request %s", request.getRequestURI());
        }

        // Need to check it before let request through.
        checkJvmRoute(request, response);

        // let the servlet invocation go through
        getNext().invoke(request, response);
    }

    public void checkJvmRoute(Request req, Response res) throws IOException, ServletException {
        String requestedId = req.getRequestedSessionId();
        HttpSession session = req.getSession(false);
        if (session != null) {
            String sessionId = session.getId();
            String jvmRoute = manager.getJvmRoute();

            if (log_.isTraceEnabled()) {
                log_.tracef("checkJvmRoute(): check if need to re-route based on JvmRoute. Session id: %s jvmRoute: %s", sessionId, jvmRoute);
            }

            if (jvmRoute != null) {
                // Check if incoming session id has JvmRoute appended. If not, append it.
                boolean setCookie = !req.isRequestedSessionIdFromURL();
                handleJvmRoute(requestedId, sessionId, jvmRoute, res, setCookie);
            }
        }
    }

    protected void handleJvmRoute(String requestedId, String sessionId, String jvmRoute, HttpServletResponse response, boolean setCookie) throws IOException {
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
            if (log_.isTraceEnabled()) {
                log_.tracef("handleJvmRoute(): We have detected a failover with different jvmRoute. old one: %s, new one: %s. Will reset the session id.", sessionJvmRoute, jvmRoute);
            }
            newId = this.manager.createSessionId(realId, this.manager.locate(realId));
        }

        if (newId != null) {
            // Fix the session's id
            resetSessionId(sessionId, newId);
        }

        // Now we know the session object has a correct id
        // Also need to ensure any session cookie is correct
        if (setCookie) {
            // Check if the jvmRoute of the requested session id matches ours.
            // Only bother if we haven't already spotted a mismatch above
            if (newId == null) {
                String requestedJvmRoute = (requestedId != null) ? this.manager.parse(requestedId).getValue() : null;

                if (!jvmRoute.equals(requestedJvmRoute)) {
                    if (log_.isTraceEnabled()) {
                        log_.tracef("handleJvmRoute(): We have detected a failover with different jvmRoute. received one: %s, new one: %s. Will reset the session id.", requestedJvmRoute, jvmRoute);
                    }
                    newId = this.manager.createSessionId(realId, this.manager.locate(realId));
                }
            }

            /* Change the sessionid cookie if needed */
            if (newId != null) {
                manager.setNewSessionCookie(newId, response);
            }
        }
    }

    /**
     * Update the id of the given session
     *
     * @param oldId id of the session to change
     * @param newId new session id the session object should have
     */
    private void resetSessionId(String oldId, String newId) throws IOException {
        ClusteredSession<? extends OutgoingDistributableSessionData> session = (ClusteredSession<?>) manager.findSession(oldId);
        // change session id with the new one using local jvmRoute.
        if (session != null) {
            // Note this will trigger a session remove from the super Tomcat class.
            session.resetIdWithRouteInfo(newId);
            if (log_.isTraceEnabled()) {
                log_.tracef("resetSessionId(): changed catalina session to= [%s] old one= [%s]", newId, oldId);
            }
        } else if (log_.isTraceEnabled()) {
            log_.tracef("resetSessionId(): no session with id %s found", newId);
        }
    }

    // Lifecycle Interface
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
}