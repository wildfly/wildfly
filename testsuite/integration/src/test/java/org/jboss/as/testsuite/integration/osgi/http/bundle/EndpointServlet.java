/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.testsuite.integration.osgi.http.bundle;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;

@SuppressWarnings("serial")
public class EndpointServlet extends HttpServlet {
    private BundleContext context;

    // This hides the default ctor and verifies that this instance is used
    public EndpointServlet(BundleContext context) {
        this.context = context;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PrintWriter out = res.getWriter();

        String testParam = req.getParameter("test");
        if ("plain".equals(testParam)) {
            out.println("Hello from Servlet");
        } else if ("initProp".equals(testParam)) {
            String value = getInitParameter(testParam);
            out.println(testParam + "=" + value);
        } else if ("context".equals(testParam)) {
            out.println(context.getBundle().getSymbolicName());
        } else {
            throw new IllegalArgumentException("Invalid 'test' parameter: " + testParam);
        }

        out.close();
    }
}
