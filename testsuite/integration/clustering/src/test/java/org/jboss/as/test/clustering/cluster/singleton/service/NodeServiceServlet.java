/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.singleton.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.group.Node;
import org.wildfly.common.function.ExceptionFunction;
import org.wildfly.service.capture.FunctionExecutor;

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
        FunctionExecutor<Supplier<Node>> executor = NodeServiceExecutorRegistry.INSTANCE.getExecutor(ServiceName.parse(serviceName));
        Instant stop = Instant.now().plus(TIMEOUT);
        ExceptionFunction<Supplier<Node>, Node, RuntimeException> function = Supplier::get;
        Node node = executor.execute(function);
        if (expected != null) {
            while (Instant.now().isBefore(stop)) {
                if ((node != null) && expected.equals(node.getName())) break;
                Thread.yield();
                node = executor.execute(function);
            }
        }
        if (node != null) {
            resp.setHeader(NODE_HEADER, node.getName());
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
