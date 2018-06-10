/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.clustering.cluster.singleton.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.service.PassiveServiceSupplier;
import org.wildfly.clustering.service.ServiceSupplier;

@WebServlet(urlPatterns = { NodeServiceServlet.SERVLET_PATH })
public class NodeServiceServlet extends HttpServlet {
    private static final long serialVersionUID = -592774116315946908L;
    public static final String NODE_HEADER = "node";
    private static final String SERVLET_NAME = "node";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    private static final String SERVICE = "service";
    private static final String EXPECTED = "expected";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    public static URI createURI(URL baseURL, ServiceName serviceName) throws URISyntaxException {
        return baseURL.toURI().resolve(buildQuery(serviceName).toString());
    }

    public static URI createURI(URL baseURL, ServiceName serviceName, String expected) throws URISyntaxException {
        return baseURL.toURI().resolve(buildQuery(serviceName).append('&').append(EXPECTED).append('=').append(expected).toString());
    }

    private static StringBuilder buildQuery(ServiceName serviceName) {
        return new StringBuilder(SERVLET_NAME).append('?').append(SERVICE).append('=').append(serviceName.getCanonicalName());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String serviceName = getRequiredParameter(req, SERVICE);
        String expected = req.getParameter(EXPECTED);
        this.log(String.format("Received request for %s, expecting %s", serviceName, expected));
        ServiceSupplier<Node> localNodeSupplier = new PassiveServiceSupplier<Node>(CurrentServiceContainer.getServiceContainer(), ServiceName.parse(serviceName)).setTimeout(TIMEOUT);
        Instant stop = Instant.now().plus(TIMEOUT);
        try {
            Node node = localNodeSupplier.get();
            if (expected != null) {
                while (Instant.now().isBefore(stop)) {
                    if ((node != null) && expected.equals(node.getName())) break;
                    Thread.yield();
                    node = localNodeSupplier.get();
                }
            }
            if (node != null) {
                resp.setHeader(NODE_HEADER, node.getName());
            }
        } catch (IllegalStateException e) {
            // Thrown when quorum was not met
        }
        resp.getWriter().write("Success");
    }

    private static String getRequiredParameter(HttpServletRequest req, String name) throws ServletException {
        String value = req.getParameter(name);
        if (value == null) {
            throw new ServletException(String.format("No %s specified", name));
        }
        return value;
    }
}
