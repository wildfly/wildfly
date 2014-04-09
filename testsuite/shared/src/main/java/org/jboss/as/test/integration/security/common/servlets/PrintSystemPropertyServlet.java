/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
