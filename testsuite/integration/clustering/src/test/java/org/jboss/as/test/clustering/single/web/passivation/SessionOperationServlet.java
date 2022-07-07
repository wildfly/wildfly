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
package org.jboss.as.test.clustering.single.web.passivation;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionActivationListener;
import jakarta.servlet.http.HttpSessionEvent;

@WebServlet(urlPatterns = SessionOperationServlet.SERVLET_PATH)
public class SessionOperationServlet extends HttpServlet {
    private static final long serialVersionUID = -1769104491085299700L;
    private static final String SERVLET_NAME = "listener";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    private static final String OPERATION = "operation";
    private static final String GET = "get";
    private static final String SET = "set";
    private static final String NAME = "name";
    private static final String VALUE = "value";
    public static final String RESULT = "result";
    public static final String SESSION_ID = "jsessionid";

    public static URI createGetURI(URL baseURL, String name) throws URISyntaxException {
        StringBuilder builder = appendParameter(buildURI(GET), NAME, name);
        return baseURL.toURI().resolve(builder.toString());
    }

    public static URI createSetURI(URL baseURL, String name, String... values) throws URISyntaxException {
        StringBuilder builder = appendParameter(buildURI(SET), NAME, name);
        for (String value: values) {
            appendParameter(builder, VALUE, value);
        }
        return baseURL.toURI().resolve(builder.toString());
    }

    private static StringBuilder buildURI(String operation) {
        return new StringBuilder(SERVLET_NAME).append('?').append(OPERATION).append('=').append(operation);
    }

    private static StringBuilder appendParameter(StringBuilder builder, String parameter, String value) {
        return builder.append('&').append(parameter).append('=').append(value);
    }

    static final BlockingQueue<Map.Entry<String, EventType>> EVENTS = new LinkedBlockingQueue<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        String operation = getRequiredParameter(req, OPERATION);
        HttpSession session = req.getSession(true);
        resp.addHeader(SESSION_ID, session.getId());
        switch (operation) {
            case SET: {
                String name = getRequiredParameter(req, NAME);
                String value = req.getParameter(VALUE);
                session.setAttribute(name, (value != null) ? new SessionAttributeValue(value) : null);
                break;
            }
            case GET: {
                String name = getRequiredParameter(req, NAME);
                SessionAttributeValue value = (SessionAttributeValue) session.getAttribute(name);
                if (value != null) {
                    resp.setHeader(RESULT, value.getValue());
                }
                break;
            }
            default: {
                throw new ServletException("Unrecognized operation: " + operation);
            }
        }

        List<Map.Entry<String, EventType>> events = new LinkedList<>();
        if (EVENTS.drainTo(events) > 0) {
            events.forEach(entry -> resp.addHeader(entry.getKey(), entry.getValue().name()));
        }
    }

    private static String getRequiredParameter(HttpServletRequest req, String name) throws ServletException {
        String value = req.getParameter(name);
        if (value == null) {
            throw new ServletException("Missing parameter: " + name);
        }
        return value;
    }

    public enum EventType {
        PASSIVATION, ACTIVATION;
    }

    public static class SessionAttributeValue implements Serializable, HttpSessionActivationListener {
        private static final long serialVersionUID = -8824497321979784527L;

        private final String value;

        public SessionAttributeValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

        @Override
        public void sessionWillPassivate(HttpSessionEvent event) {
            EVENTS.add(new SimpleImmutableEntry<>(event.getSession().getId(), EventType.PASSIVATION));
        }

        @Override
        public void sessionDidActivate(HttpSessionEvent event) {
            EVENTS.add(new SimpleImmutableEntry<>(event.getSession().getId(), EventType.ACTIVATION));
        }
    }
}
