/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility servlet that delegates to an EJB to perform topology stabilization.
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