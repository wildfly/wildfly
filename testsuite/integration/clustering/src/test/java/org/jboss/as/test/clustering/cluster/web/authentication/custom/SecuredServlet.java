/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.web.authentication.custom;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { SecuredServlet.SERVLET_PATH })
@ServletSecurity(@HttpConstraint(rolesAllowed = { "allowed" }))
public class SecuredServlet extends HttpServlet {
    private static final long serialVersionUID = -7525845342219183894L;

    private static final String SERVLET_NAME = "secured";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String PRINCIPAL_HEADER = "principal";

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader(PRINCIPAL_HEADER, request.getUserPrincipal().getName());
    }
}
