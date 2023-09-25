/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.web.authentication;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = { SecureServlet.SERVLET_PATH })
@ServletSecurity(@HttpConstraint(rolesAllowed = { "allowed" }))
public class SecureServlet extends HttpServlet {
    private static final long serialVersionUID = -6366000117528193641L;

    private static final String SERVLET_NAME = "secure";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String SESSION_ID_HEADER = "session-id";

    public static URI createURI(URL baseURL) throws URISyntaxException {
        return baseURL.toURI().resolve(SERVLET_NAME);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = "";
        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionId = (String) session.getAttribute(SESSION_ID_HEADER);
            if (sessionId == null) {
                sessionId = session.getId();
                session.setAttribute(SESSION_ID_HEADER, sessionId);
            }
        }
        response.setHeader(SESSION_ID_HEADER, sessionId);
    }
}
