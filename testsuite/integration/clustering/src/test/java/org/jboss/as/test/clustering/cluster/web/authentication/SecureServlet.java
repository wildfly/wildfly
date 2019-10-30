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
package org.jboss.as.test.clustering.cluster.web.authentication;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
