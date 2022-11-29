/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.wildfly.test.security.servlets;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Permission;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.evidence.PasswordGuessEvidence;

/**
 * Servlet which checks if given identity has given permission in current Elytron security domain. If the {@value #PARAM_USER}
 * request parameter is not provided then an anonymous identity is used, otherwise the identity is retrieved by calling
 * {@link org.wildfly.security.auth.server.SecurityDomain#authenticate(String, org.wildfly.security.evidence.Evidence)} method
 * with {@value #PARAM_PASSWORD} request parameter used as the Evidence.
 * <p>
 * The checked permission is specified by request parameters {@value #PARAM_CLASS}, {@value #PARAM_TARGET} and
 * {@value #PARAM_ACTION}.
 * </p>
 * <p>
 * Response body in normal cases contains just "true" or "false" String. If authentication to security domain fails, then status
 * code {@link HttpServletResponse#SC_FORBIDDEN} is used for the response. If the check permission class parameter is missing
 * then status code {@link HttpServletResponse#SC_BAD_REQUEST} is used for the response.
 * </p>
 *
 * @author Josef Cacek
 */
@WebServlet(CheckIdentityPermissionServlet.SERVLET_PATH)
public class CheckIdentityPermissionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/checkIdentityPermission";

    public static final String PARAM_USER = "user";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_CLASS = "class";
    public static final String PARAM_TARGET = "target";
    public static final String PARAM_ACTION = "action";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");

        SecurityIdentity si = null;
        final String user = req.getParameter(PARAM_USER);

        if (user != null) {
            final String password = req.getParameter(PARAM_PASSWORD);
            try {
                si = SecurityDomain.getCurrent().authenticate(user, new PasswordGuessEvidence(password.toCharArray()));
            } catch (Exception e) {
                e.printStackTrace();
                resp.sendError(SC_FORBIDDEN, e.getMessage());
                return;
            }
        } else {
            si = SecurityDomain.getCurrent().getCurrentSecurityIdentity();
        }

        String className = req.getParameter(PARAM_CLASS);
        if (className == null) {
            resp.sendError(SC_BAD_REQUEST, "Parameter class has to be provided");
            return;
        }
        String target = req.getParameter(PARAM_TARGET);
        String action = req.getParameter(PARAM_ACTION);

        Permission perm = null;
        try {
            if (target == null) {
                perm = (Permission) Class.forName(className).newInstance();
            } else if (action == null) {
                perm = (Permission) Class.forName(className).getConstructor(String.class).newInstance(target);
            } else {
                perm = (Permission) Class.forName(className).getConstructor(String.class, String.class).newInstance(target,
                        action);
            }
        } catch (Exception e) {
            throw new ServletException("Unable to create permission instance", e);
        }

        final PrintWriter writer = resp.getWriter();
        writer.print(si.implies(perm));
        writer.close();
    }
}
