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

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

import javax.annotation.security.DeclareRoles;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
