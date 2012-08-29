/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.osgi.webapp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author David Bosschaert
 */
@WebServlet(name = "TestServletContext", urlPatterns = { "/testservletcontext" })
public class TestServletContext extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();

        String testValue = "set on " + System.currentTimeMillis();
        getServletContext().setAttribute(getClass().getName(), testValue);

        BundleContext ctxt = (BundleContext)
                getServletContext().getAttribute("osgi-bundlecontext");


        try {
            Filter filter = ctxt.createFilter("(&(objectClass=" + ServletContext.class.getName() + ")" +
            		"(osgi.web.symbolicname=" + ctxt.getBundle().getSymbolicName() + "))");

            ServiceTracker st = new ServiceTracker(ctxt, filter, null);
            st.open();

            ServletContext sc = (ServletContext) st.waitForService(2000);
            ServiceReference sr = st.getServiceReference();

            if (sc == null) {
                writer.write("ServletContext service not found given filter: " + filter);
                return;
            }

            // Cannot test the ServletContext on equality, because it might be wrapped, so we'll test a value set in it instead
            Object attrVal = sc.getAttribute(getClass().getName());
            if (!testValue.equals(attrVal)) {
                writer.write("Error: Servlet Context service not the same as the actual Servlet Context: " + attrVal);
                return;
            }

            writer.write("ServletContext: " + sr.getProperty("osgi.web.symbolicname") + "|" + sr.getProperty("osgi.web.contextpath"));
            st.close();
        } catch (Exception e) {
            e.printStackTrace(writer);
        } finally {
            writer.close();
        }
    }
}
