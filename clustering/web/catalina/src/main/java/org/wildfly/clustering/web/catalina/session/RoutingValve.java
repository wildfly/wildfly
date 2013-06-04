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
package org.wildfly.clustering.web.catalina.session;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.jboss.logging.Logger;
import org.wildfly.clustering.web.session.RoutingSupport;

/**
 * Responsible for parsing routing information from requested session identifier
 * and formatting the session identifier with the appropriate routing information
 * after the request is complete.
 * @author Paul Ferraro
 */
public class RoutingValve extends ValveBase {
    private static final Logger log = Logger.getLogger(RoutingValve.class);

    private final RoutingSupport support;

    public RoutingValve(RoutingSupport support) {
        this.support = support;
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        if (log.isTraceEnabled()) {
            log.tracef("Handling request %s", request.getRequestURI());
        }

        String requestedSessionId = request.getRequestedSessionId();
        if (requestedSessionId != null) {
            Map.Entry<String, String> entry = this.support.parse(requestedSessionId);
            String sessionId = entry.getKey();

            if (!requestedSessionId.equals(sessionId)) {
                request.setRequestedSessionId(sessionId);
            }
        }

        this.next.invoke(request, response);

        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionId = session.getId();
            request.changeSessionId(this.support.format(sessionId, this.support.locate(sessionId)));
        }
    }
}
