/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.common.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse)
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
