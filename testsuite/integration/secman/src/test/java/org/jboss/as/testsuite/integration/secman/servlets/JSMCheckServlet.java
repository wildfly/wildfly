/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.testsuite.integration.secman.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet, which checks if the Java Security Manager (JSM) is enabled. Response is a plain text "true" when JSM is enabled or
 * "false" otherwise.
 *
 * @author Josef Cacek
 */
@WebServlet(JSMCheckServlet.SERVLET_PATH)
public class JSMCheckServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/JSMCheckServlet";

    /**
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
        writer.write(Boolean.toString(System.getSecurityManager() != null));
        writer.close();
    }
}
