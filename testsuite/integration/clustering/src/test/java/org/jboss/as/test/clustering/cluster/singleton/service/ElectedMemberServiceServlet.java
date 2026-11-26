/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.singleton.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.GroupMember;

@WebServlet(urlPatterns = { ElectedMemberServiceServlet.SERVLET_PATH })
public class ElectedMemberServiceServlet extends HttpServlet {
    private static final long serialVersionUID = -592774116315946908L;
    public static final String ELECTED_MEMBER_HEADER = "elected-member";
    private static final String SERVLET_NAME = "elected-member";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    private static final String SERVICE = "service";
    private static final String EXPECTED = "expected";

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
        GroupMember elected = ElectedMemberRegistry.INSTANCE.getReference(ServiceName.parse(serviceName)).get();
        if (elected != null) {
            resp.setHeader(ELECTED_MEMBER_HEADER, elected.getName());
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
