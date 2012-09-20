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
package org.jboss.as.test.integration.osgi.webapp.bundle;

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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author David Bosschaert
 */
@WebServlet(name = "TestServletContext", urlPatterns = { "/testservletcontext" })
public class TestServletContext extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("GET " + req);

        String testValue = "set on " + System.currentTimeMillis();
        PrintWriter writer = resp.getWriter();
        ServletContext servletContext = getServletContext();
        servletContext.setAttribute(getClass().getName(), testValue);

        BundleContext bundleContext = (BundleContext) servletContext.getAttribute("osgi-bundlecontext");
        if (bundleContext == null)
            throw new RuntimeException("Error: BundleContext attribute not set on: " + servletContext);

        try {
            String symbolicName = bundleContext.getBundle().getSymbolicName();
            Filter filter = bundleContext.createFilter("(&(objectClass=" + ServletContext.class.getName() + ")" + "(osgi.web.symbolicname=" + symbolicName + "))");

            ServiceTracker tracker = new ServiceTracker(bundleContext, filter, null);
            tracker.open();

            ServletContext servletContextService = (ServletContext) tracker.waitForService(2000);
            if (servletContextService == null)
                throw new RuntimeException("ServletContext service not found given filter: " + filter);

            // Cannot test the ServletContext on equality, because it might be wrapped, so we'll test a value set in it instead
            Object attrVal = servletContextService.getAttribute(getClass().getName());
            if (!testValue.equals(attrVal))
                throw new RuntimeException("Error: Servlet Context service not the same as the actual Servlet Context: " + attrVal);

            ServiceReference sref = tracker.getServiceReference();
            writer.write("ServletContext: " + sref.getProperty("osgi.web.symbolicname") + "|" + sref.getProperty("osgi.web.contextpath"));
            tracker.close();
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            writer.close();
        }
    }

    private static final long serialVersionUID = 1L;
}
