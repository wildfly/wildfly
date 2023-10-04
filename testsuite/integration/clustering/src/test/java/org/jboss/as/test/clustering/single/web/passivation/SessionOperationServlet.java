/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.web.passivation;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionActivationListener;
import jakarta.servlet.http.HttpSessionEvent;

import org.wildfly.clustering.web.annotation.Immutable;

@WebServlet(urlPatterns = SessionOperationServlet.SERVLET_PATH)
public class SessionOperationServlet extends HttpServlet {
    private static final long serialVersionUID = -1769104491085299700L;
    private static final String SERVLET_NAME = "listener";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    private static final String NAME = "name";
    private static final String VALUE = "value";
    public static final String RESULT = "result";
    public static final String SESSION_ID = "jsessionid";

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME);
    }

    public static URI createURI(URL baseURL, String name, String... values) throws URISyntaxException {
        StringBuilder builder = buildURI(name);
        for (String value: values) {
            appendParameter(builder, VALUE, value);
        }
        return baseURL.toURI().resolve(builder.toString());
    }

    private static StringBuilder buildURI(String name) {
        return new StringBuilder(SERVLET_NAME).append('?').append(NAME).append('=').append(name);
    }

    private static StringBuilder appendParameter(StringBuilder builder, String parameter, String value) {
        return builder.append('&').append(parameter).append('=').append(value);
    }

    static final BlockingQueue<Map.Entry<String, EventType>> EVENTS = new LinkedBlockingQueue<>();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String> cookies = Optional.ofNullable(req.getCookies()).map(Arrays::asList).orElse(List.of()).stream().collect(Collectors.toMap(Cookie::getName, Cookie::getValue));
        this.getServletContext().log(String.format("[%s]\t%s\t%s?%s", req.getMethod(), cookies, req.getRequestURI(), req.getQueryString()));
        super.service(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            resp.addHeader(SESSION_ID, session.getId());
            String name = getRequiredParameter(req, NAME);
            SessionAttributeValue value = (SessionAttributeValue) session.getAttribute(name);
            if (value != null) {
                resp.setHeader(RESULT, value.getValue());
            }
        }

        List<Map.Entry<String, EventType>> events = new LinkedList<>();
        if (EVENTS.drainTo(events) > 0) {
            events.forEach(entry -> resp.addHeader(entry.getKey(), entry.getValue().name()));
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        HttpSession session = req.getSession(true);
        resp.addHeader(SESSION_ID, session.getId());
        String name = getRequiredParameter(req, NAME);
        String value = req.getParameter(VALUE);
        session.setAttribute(name, (value != null) ? new SessionAttributeValue(value) : null);

        List<Map.Entry<String, EventType>> events = new LinkedList<>();
        if (EVENTS.drainTo(events) > 0) {
            events.forEach(entry -> resp.addHeader(entry.getKey(), entry.getValue().name()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
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

    @Immutable
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
            System.out.println("HttpSessionActivationListener.sessionWillPassivate(" + event.getSession().getId() + ")");
            EVENTS.add(new SimpleImmutableEntry<>(event.getSession().getId(), EventType.PASSIVATION));
        }

        @Override
        public void sessionDidActivate(HttpSessionEvent event) {
            System.out.println("HttpSessionActivationListener.sessionDidActivate(" + event.getSession().getId() + ")");
            EVENTS.add(new SimpleImmutableEntry<>(event.getSession().getId(), EventType.ACTIVATION));
        }
    }
}
