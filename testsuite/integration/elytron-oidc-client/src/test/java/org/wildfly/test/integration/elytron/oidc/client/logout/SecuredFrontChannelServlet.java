/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.oidc.client.logout;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.annotation.security.DeclareRoles;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;

/**
 * Protected version of {@link SimpleServlet}. Only {@value #ALLOWED_ROLE} role has access right.
 *
 * @author Josef Cacek
 */
@DeclareRoles({ SecuredFrontChannelServlet.ALLOWED_ROLE })
@ServletSecurity(@HttpConstraint(rolesAllowed = { SecuredFrontChannelServlet.ALLOWED_ROLE }))
@WebServlet(SecuredFrontChannelServlet.SERVLET_PATH)
public class SecuredFrontChannelServlet extends SimpleServlet {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;
    public static final String SERVLET_PATH = "/SecuredFrontChannelServlet";
    public static final String ALLOWED_ROLE = "JBossAdmin";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (PrintWriter writer = resp.getWriter();){
            writer.println("<html>");
            writer.println("  <head><title>" + SERVLET_PATH + "</title></head>");
            writer.println("  <body>");
            writer.println("    <h1>" + SERVLET_PATH + "</h1>");
            writer.println("  </body>");
            writer.println("</html>");
        }
    }
}