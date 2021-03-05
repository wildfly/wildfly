/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.single.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Function;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { SimpleServlet.SERVLET_PATH })
public class SimpleServlet extends HttpServlet {
    private static final long serialVersionUID = -592774116315946908L;
    private static final String SERVLET_NAME = "simple";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String REQUEST_DURATION_PARAM = "requestduration";
    public static final String HEADER_SERIALIZED = "serialized";
    public static final String VALUE_HEADER = "value";
    public static final String SESSION_ID_HEADER = "sessionId";
    public static final String ATTRIBUTE = "test";
    public static final String HEADER_NODE_NAME = "nodename";

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return createURI(baseURL.toURI());
    }

    public static URI createURI(URI baseURI) {
        return baseURI.resolve(SERVLET_NAME);
    }

    public static URI createURI(URL baseURL, int requestDuration) throws URISyntaxException {
        return createURI(baseURL.toURI(), requestDuration);
    }

    public static URI createURI(URI baseURI, int requestDuration) {
        return baseURI.resolve(SERVLET_NAME + '?' + REQUEST_DURATION_PARAM + '=' + requestDuration);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.getServletContext().log(String.format("[%s] %s?%s", request.getMethod(), request.getRequestURI(), request.getQueryString()));
        super.service(request, response);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            response.addHeader(SESSION_ID_HEADER, session.getId());
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        response.addHeader(SESSION_ID_HEADER, session.getId());
        session.removeAttribute(ATTRIBUTE);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Function<HttpSession, Mutable> accessor = session -> {
            Mutable mutable = (Mutable) session.getAttribute(ATTRIBUTE);
            if (mutable == null) {
                mutable = new Mutable(0);
                session.setAttribute(ATTRIBUTE, mutable);
            }
            return mutable;
        };
        this.increment(request, response, accessor);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Function<HttpSession, Mutable> accessor = session -> {
            Mutable mutable = (Mutable) session.getAttribute(ATTRIBUTE);
            if (mutable == null) {
                session.setAttribute(ATTRIBUTE, new Mutable(0));
            }
            return (mutable == null) ? (Mutable) session.getAttribute(ATTRIBUTE) : mutable;
        };
        this.increment(request, response, accessor);
    }

    private void increment(HttpServletRequest request, HttpServletResponse response, Function<HttpSession, Mutable> accessor) throws IOException {
        HttpSession session = request.getSession(true);
        response.addHeader(SESSION_ID_HEADER, session.getId());
        Mutable mutable = accessor.apply(session);
        int value = mutable.increment();
        response.setIntHeader(VALUE_HEADER, value);
        response.setHeader(HEADER_SERIALIZED, Boolean.toString(mutable.wasSerialized()));

        Mutable current = (Mutable) session.getAttribute(ATTRIBUTE);
        if (!mutable.equals(current)) {
            throw new IllegalStateException(String.format("Session attribute value = %s, expected %s", current, mutable));
        }

        try {
            String nodeName = System.getProperty("jboss.node.name");
            response.setHeader(HEADER_NODE_NAME, nodeName);
        } catch (Exception ignore) {
        }

        this.getServletContext().log(request.getRequestURI() + ", value = " + value);

        // Long running request?
        if (request.getParameter(REQUEST_DURATION_PARAM) != null) {
            int duration = Integer.parseInt(request.getParameter(REQUEST_DURATION_PARAM));
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        response.getWriter().write("Success");
    }
}
