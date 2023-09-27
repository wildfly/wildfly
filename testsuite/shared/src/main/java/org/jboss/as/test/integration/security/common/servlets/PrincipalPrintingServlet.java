/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.common.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A servlet which reports the name of the callers principal.
 *
 * @author JanLanik
 */
@WebServlet(name = "PrincipalPrintingServlet", urlPatterns = { PrincipalPrintingServlet.SERVLET_PATH })
public class PrincipalPrintingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/printPrincipal";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        final PrintWriter writer = resp.getWriter();
        final Principal principal = req.getUserPrincipal();
        if (null == principal) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Principal name is printed only for the authenticated users.");
        } else {
            writer.write(principal.getName());
        }
        writer.close();
    }
}
