/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Utility servlet that delegates to an Jakarta Enterprise Beans to perform topology stabilization.
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { TopologyChangeListenerServlet.SERVLET_PATH })
public class TopologyChangeListenerServlet extends HttpServlet {
    private static final long serialVersionUID = -4382952409558738642L;
    private static final String SERVLET_NAME = "membership";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    private static final String CONTAINER = "container";
    private static final String CACHE = "cache";
    private static final String NODES = "nodes";
    private static final String TIMEOUT = "timeout";

    public static URI createURI(URL baseURL, String container, String cache, long timeout, String... nodes) throws URISyntaxException {
        StringBuilder builder = new StringBuilder(baseURL.toURI().resolve(SERVLET_NAME).toString());
        builder.append('?').append(CONTAINER).append('=').append(container);
        builder.append('&').append(CACHE).append('=').append(cache);
        builder.append('&').append(TIMEOUT).append('=').append(timeout);
        for (String node: nodes) {
            builder.append('&').append(NODES).append('=').append(node);
        }
        return URI.create(builder.toString());
    }

    @EJB
    private TopologyChangeListener listener;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String container = getRequiredParameter(request, CONTAINER);
        String cache = getRequiredParameter(request, CACHE);
        String[] nodes = request.getParameterValues(NODES);
        long timeout = parseLong(getRequiredParameter(request, TIMEOUT));
        try {
            this.listener.establishTopology(container, cache, timeout, nodes);
        } catch (InterruptedException e) {
            throw new ServletException(e);
        }
    }

    private static String getRequiredParameter(HttpServletRequest request, String name) throws ServletException {
        String value = request.getParameter(name);
        if (value == null) {
            throw new ServletException(String.format("No '%s' parameter specified", name));
        }
        return value;
    }

    private static long parseLong(String value) throws ServletException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ServletException(String.format("Value '%s' cannot be parsed to long.", value), e);
        }
    }
}
