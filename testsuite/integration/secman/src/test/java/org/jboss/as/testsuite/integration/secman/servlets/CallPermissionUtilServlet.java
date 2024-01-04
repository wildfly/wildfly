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

import org.jboss.as.testsuite.integration.secman.PermissionUtil;

/**
 * Servlet which calls method(s) from {@link PermissionUtil} class usually packaged in a library (i.e. another archive).
 *
 * @author Josef Cacek
 */
@WebServlet(CallPermissionUtilServlet.SERVLET_PATH)
public class CallPermissionUtilServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/CallPermissionUtilServlet";
    public static final String PARAM_PROPERTY_NAME = "property";
    public static final String DEFAULT_PROPERTY_NAME = "java.home";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        String property = req.getParameter(PARAM_PROPERTY_NAME);
        if (property == null || property.length() == 0) {
            property = DEFAULT_PROPERTY_NAME;
        }
        final PrintWriter writer = resp.getWriter();
        writer.write(PermissionUtil.getSystemProperty(property));
        writer.close();
    }
}
