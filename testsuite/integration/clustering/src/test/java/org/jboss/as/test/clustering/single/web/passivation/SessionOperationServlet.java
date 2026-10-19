/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.web.passivation;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionActivationListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import org.wildfly.clustering.web.annotation.Immutable;

/**
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
@WebServlet(urlPatterns = SessionOperationServlet.SERVLET_PATH)
public class SessionOperationServlet extends HttpServlet {
    @Serial
    private static final long serialVersionUID = -1769104491085299700L;
    private static final String SERVLET_NAME = "listener";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String NAME = "name";
    private static final String VALUE = "value";
    public static final String RESULT = "result";
    public static final String SESSION_ID = "jsessionid";
    public static final String ACTIVATIONS = "activations";
    public static final String PASSIVATIONS = "passivation";

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL, Map.of());
    }

    public static URI createURI(URL baseURL, String name, String... values) throws URISyntaxException {
        return createURI(baseURL, Map.of(NAME, List.of(name), VALUE, List.of(values)));
    }

    public static URI createURI(URL baseURL, Map<String, List<String>> parameters) throws URISyntaxException {
        StringBuilder builder = new StringBuilder(SERVLET_NAME);
        if (!parameters.isEmpty()) {
            char delimiter = '?';
            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                String key = entry.getKey();
                for (String value : entry.getValue()) {
                    builder.append(delimiter).append(key).append('=').append(value);
                    delimiter = '&';
                }
            }
        }
        return baseURL.toURI().resolve(builder.toString());
    }

    @Override
    public void init() {
        this.getServletContext().setAttribute(ACTIVATIONS, new ConcurrentHashMap<>());
        this.getServletContext().setAttribute(PASSIVATIONS, new ConcurrentHashMap<>());
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String> cookies = Optional.ofNullable(req.getCookies()).map(Stream::of).orElse(Stream.of()).collect(Collectors.toMap(Cookie::getName, Cookie::getValue));
        this.getServletContext().log(String.format("[%s]\t%s\t%s?%s", req.getMethod(), cookies, req.getRequestURI(), req.getQueryString()));
        super.service(req, resp);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            response.addHeader(SESSION_ID, session.getId());
            String sessionId = Optional.ofNullable(request.getParameter(SESSION_ID)).orElse(session.getId());
            this.recordPassivationEvents(sessionId, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            response.addHeader(SESSION_ID, session.getId());
            String name = getRequiredParameter(request, NAME);
            SessionAttributeValue value = (SessionAttributeValue) session.getAttribute(name);
            if (value != null) {
                response.setHeader(RESULT, value.getValue());
            }
            this.recordPassivationEvents(session.getId(), response);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        HttpSession session = request.getSession(true);
        response.addHeader(SESSION_ID, session.getId());
        String name = getRequiredParameter(request, NAME);
        String value = request.getParameter(VALUE);
        session.setAttribute(name, (value != null) ? new SessionAttributeValue(value) : null);
        this.recordPassivationEvents(session.getId(), response);
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

    private void recordPassivationEvents(String sessionId, HttpServletResponse response) {
        for (Instant activation : Optional.<Iterable<Instant>>ofNullable(getActivations(this.getServletContext()).get(sessionId)).orElse(List.of())) {
            response.addHeader(ACTIVATIONS, Long.toString(activation.toEpochMilli()));
        }
        for (Instant passivation : Optional.<Iterable<Instant>>ofNullable(getPassivations(this.getServletContext()).get(sessionId)).orElse(List.of())) {
            response.addHeader(PASSIVATIONS, Long.toString(passivation.toEpochMilli()));
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Queue<Instant>> getActivations(ServletContext context) {
        return (Map<String, Queue<Instant>>) context.getAttribute(ACTIVATIONS);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Queue<Instant>> getPassivations(ServletContext context) {
        return (Map<String, Queue<Instant>>) context.getAttribute(PASSIVATIONS);
    }

    @Immutable
    public static class SessionAttributeValue implements HttpSessionActivationListener, Serializable {
        @Serial
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
            System.out.println(String.format("HttpSessionActivationListener.sessionWillPassivate(%s)", event.getSession().getId()));
            HttpSession session = event.getSession();
            getPassivations(session.getServletContext()).computeIfAbsent(session.getId(), key -> new LinkedBlockingQueue<>()).add(Instant.now());
        }

        @Override
        public void sessionDidActivate(HttpSessionEvent event) {
            System.out.println(String.format("HttpSessionActivationListener.sessionDidActivate(%s)", event.getSession().getId()));
            HttpSession session = event.getSession();
            getActivations(session.getServletContext()).computeIfAbsent(session.getId(), key -> new LinkedBlockingQueue<>()).add(Instant.now());
        }
    }

    @WebListener
    public static class PassivationSessionListener implements HttpSessionListener {

        @Override
        public void sessionCreated(HttpSessionEvent event) {
            HttpSession session = event.getSession();
            getActivations(session.getServletContext()).put(session.getId(), new LinkedBlockingQueue<>());
            getPassivations(session.getServletContext()).put(session.getId(), new LinkedBlockingQueue<>());
        }

        @Override
        public void sessionDestroyed(HttpSessionEvent event) {
            HttpSession session = event.getSession();
            getActivations(session.getServletContext()).remove(session.getId());
            getPassivations(session.getServletContext()).remove(session.getId());
        }
    }
}
