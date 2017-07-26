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
package org.jboss.as.test.clustering.cluster.web.expiration;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

@WebServlet(urlPatterns = SessionOperationServlet.SERVLET_PATH)
public class SessionOperationServlet extends HttpServlet {
    private static final long serialVersionUID = -1769104491085299700L;
    private static final String SERVLET_NAME = "listener";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    private static final String OPERATION = "operation";
    private static final String INVALIDATE = "invalidate";
    private static final String GET = "get";
    private static final String SET = "set";
    private static final String REMOVE = "remove";
    private static final String TIMEOUT = "timeout";
    private static final String NAME = "name";
    private static final String VALUE = "value";
    public static final String RESULT = "result";
    public static final String SESSION_ID = "jsessionid";
    public static final String CREATED_SESSIONS = "created";
    public static final String DESTROYED_SESSIONS = "destroyed";
    public static final String ADDED_ATTRIBUTES = "added";
    public static final String REPLACED_ATTRIBUTES = "replaced";
    public static final String REMOVED_ATTRIBUTES = "removed";
    public static final String BOUND_ATTRIBUTES = "bound";
    public static final String UNBOUND_ATTRIBUTES = "unbound";

    public static URI createGetURI(URL baseURL, String name) throws URISyntaxException {
        return createGetURI(baseURL, name, null);
    }

    public static URI createGetURI(URL baseURL, String name, String value) throws URISyntaxException {
        StringBuilder builder = appendParameter(buildURI(GET), NAME, name);
        if (value != null) {
            appendParameter(builder, VALUE, value);
        }
        return baseURL.toURI().resolve(builder.toString());
    }

    public static URI createSetURI(URL baseURL, String name, String... values) throws URISyntaxException {
        StringBuilder builder = appendParameter(buildURI(SET), NAME, name);
        for (String value: values) {
            appendParameter(builder, VALUE, value);
        }
        return baseURL.toURI().resolve(builder.toString());
    }

    public static URI createRemoveURI(URL baseURL, String name) throws URISyntaxException {
        return baseURL.toURI().resolve(appendParameter(buildURI(REMOVE), NAME, name).toString());
    }

    public static URI createInvalidateURI(URL baseURL) throws URISyntaxException {
        return baseURL.toURI().resolve(buildURI(INVALIDATE).toString());
    }

    public static URI createTimeoutURI(URL baseURL, int timeout) throws URISyntaxException {
        return baseURL.toURI().resolve(appendParameter(buildURI(TIMEOUT), TIMEOUT, Integer.toString(timeout)).toString());
    }

    private static StringBuilder buildURI(String operation) {
        return new StringBuilder(SERVLET_NAME).append('?').append(OPERATION).append('=').append(operation);
    }

    private static StringBuilder appendParameter(StringBuilder builder, String parameter, String value) {
        return builder.append('&').append(parameter).append('=').append(value);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        String operation = getRequiredParameter(req, OPERATION);
        HttpSession session = req.getSession(true);
        resp.addHeader(SESSION_ID, session.getId());
        //System.out.println(String.format("%s?%s;jsessionid=%s", req.getRequestURL(), req.getQueryString(), session.getId()));
        if (operation.equals(SET)) {
            String name = getRequiredParameter(req, NAME);
            String[] values = req.getParameterValues(VALUE);
            if (values != null) {
                SessionAttributeValue value = new SessionAttributeValue(values[0]);
                session.setAttribute(name, value);
                for (int i = 1; i < values.length; ++i) {
                    value.setValue(values[i]);
                }
            } else {
                session.setAttribute(name, null);
            }
        } else if (operation.equals(REMOVE)) {
            String name = getRequiredParameter(req, NAME);
            session.removeAttribute(name);
        } else if (operation.equals(INVALIDATE)) {
            session.invalidate();
        } else if (operation.equals(GET)) {
            String name = getRequiredParameter(req, NAME);
            SessionAttributeValue value = (SessionAttributeValue) session.getAttribute(name);
            if (value != null) {
                resp.setHeader(RESULT, value.getValue());
                String newValue = req.getParameter(VALUE);
                if (newValue != null) {
                    value.setValue(newValue);
                }
            }
        } else if (operation.equals(TIMEOUT)) {
            String timeout = getRequiredParameter(req, TIMEOUT);
            session.setMaxInactiveInterval(Integer.parseInt(timeout));
        } else {
            throw new ServletException("Unrecognized operation: " + operation);
        }

        setHeader(resp, CREATED_SESSIONS, RecordingWebListener.createdSessions);
        setHeader(resp, DESTROYED_SESSIONS, RecordingWebListener.destroyedSessions);
        setHeader(resp, ADDED_ATTRIBUTES, RecordingWebListener.addedAttributes.get(session.getId()));
        setHeader(resp, REPLACED_ATTRIBUTES, RecordingWebListener.replacedAttributes.get(session.getId()));
        setHeader(resp, REMOVED_ATTRIBUTES, RecordingWebListener.removedAttributes.get(session.getId()));
        setHeader(resp, BOUND_ATTRIBUTES, SessionAttributeValue.boundAttributes);
        setHeader(resp, UNBOUND_ATTRIBUTES, SessionAttributeValue.unboundAttributes);
    }

    private static void setHeader(HttpServletResponse response, String header, BlockingQueue<String> queue) {
        if (queue != null) {
            List<String> values = new LinkedList<>();
            if (queue.drainTo(values) > 0) {
                for (String value: values) {
                    response.addHeader(header, value);
                }
            }
        }
    }

    private static String getRequiredParameter(HttpServletRequest req, String name) throws ServletException {
        String value = req.getParameter(name);
        if (value == null) {
            throw new ServletException("Missing parameter: " + name);
        }
        return value;
    }

    public static class SessionAttributeValue implements Serializable, HttpSessionBindingListener {
        private static final long serialVersionUID = -8824497321979784527L;
        static final BlockingQueue<String> boundAttributes = new LinkedBlockingQueue<>();
        static final BlockingQueue<String> unboundAttributes = new LinkedBlockingQueue<>();

        private volatile String value;

        public SessionAttributeValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public void valueBound(HttpSessionBindingEvent event) {
            boundAttributes.add(((SessionAttributeValue) event.getValue()).getValue());
        }

        @Override
        public void valueUnbound(HttpSessionBindingEvent event) {
            unboundAttributes.add(((SessionAttributeValue) event.getValue()).getValue());
        }
    }
}
