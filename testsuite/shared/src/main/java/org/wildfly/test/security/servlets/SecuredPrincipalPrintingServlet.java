/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.security.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

import jakarta.annotation.security.DeclareRoles;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A secured servlet that gets request assigned principal ({@link HttpServletRequest#getUserPrincipal()}) and prints its name.
 *
 * @author Josef Cacek
 */
@DeclareRoles({ SecuredPrincipalPrintingServlet.ALLOWED_ROLE })
@ServletSecurity(@HttpConstraint(rolesAllowed = { SecuredPrincipalPrintingServlet.ALLOWED_ROLE }))
@WebServlet(SecuredPrincipalPrintingServlet.SERVLET_PATH)
public class SecuredPrincipalPrintingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/principal";
    public static final String ALLOWED_ROLE = "JBossAdmin";

    /**
     * Writes principal name.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        Principal userPrincipal = req.getUserPrincipal();
        if (userPrincipal != null) {
            final PrintWriter writer = resp.getWriter();
            writer.print(userPrincipal.getName());
            writer.close();
        }
    }
}
