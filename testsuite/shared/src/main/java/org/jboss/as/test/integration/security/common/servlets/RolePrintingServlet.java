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
package org.jboss.as.test.integration.security.common.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A RolePrintingServlet gets list of role names as (GET) request parameters and returns a comma separated sublist of such role
 * names for which {@link HttpServletRequest#isUserInRole(String)} returns <code>true</code>. Don't forget to declare the tested
 * roles in the web.xml file.
 *
 * @author Josef Cacek
 */
@WebServlet(urlPatterns = { RolePrintingServlet.SERVLET_PATH })
@ServletSecurity(@HttpConstraint(rolesAllowed = { "*" }))
public class RolePrintingServlet extends HttpServlet {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** Name of the HTTP request parameter which holds a role name. */
    public static String PARAM_ROLE_NAME = "role";
    /** The default servlet path (used in {@link WebServlet} annotation). */
    public static final String SERVLET_PATH = "/printRoles";

    // Protected methods -----------------------------------------------------

    /**
     * Writes plain-text response with the comma-separated role names (subset of the names retrieved as GET parameters).
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        final PrintWriter writer = resp.getWriter();
        final String[] roleNames = req.getParameterValues(PARAM_ROLE_NAME);
        //start with the comma to simplify exact search (e.g. for role 'admin' an user can search for ',admin,')
        writer.write(",");
        if (roleNames != null) {
            for (final String role : roleNames) {
                if (req.isUserInRole(role)) {
                    writer.write(role + ",");
                }
            }
        }
        writer.close();
    }
}
