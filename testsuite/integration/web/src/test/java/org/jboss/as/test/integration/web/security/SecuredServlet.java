/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security;

import java.io.IOException;
import java.io.Writer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A simple servlet that just writes back a string
 *
 * @author Anil Saldhana
 */
@WebServlet(name = "SecuredServlet", urlPatterns = {"/secured/"}, loadOnStartup = 1)
@ServletSecurity(@HttpConstraint(rolesAllowed = {"gooduser"}))
public class SecuredServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Writer writer = resp.getWriter();
        writer.write("GOOD\n");
        writer.write("Remote user: " + req.getRemoteUser() + "\n");
        writer.write("User principal: " + req.getUserPrincipal() + "\n");
        writer.write("Authentication type: " + req.getAuthType());
    }
}
