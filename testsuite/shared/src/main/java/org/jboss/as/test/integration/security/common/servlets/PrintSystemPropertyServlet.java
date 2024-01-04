/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.security.common.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet, which prints value of system property. By default it prints value of property {@value #DEFAULT_PROPERTY_NAME}, but
 * you can specify another property name by using request parameter {@value #PARAM_PROPERTY_NAME}.
 *
 * @author Josef Cacek
 */
@WebServlet(PrintSystemPropertyServlet.SERVLET_PATH)
public class PrintSystemPropertyServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/SysPropServlet";
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
        writer.write(System.getProperty(property, ""));
        writer.close();
    }
}
