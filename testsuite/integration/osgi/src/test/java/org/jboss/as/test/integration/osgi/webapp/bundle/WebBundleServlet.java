/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
import java.io.Writer;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

@SuppressWarnings("serial")
@WebServlet(name = "WebBundleServlet", urlPatterns = { "/servlet" })
public class WebBundleServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String bundleSource = "";
        ClassLoader classLoader = WebBundleServlet.class.getClassLoader();
        try {
            if (classLoader instanceof BundleReference) {
                Bundle bundle = ((BundleReference)classLoader).getBundle();
                bundleSource = " from " + bundle.getSymbolicName();
            }
        } catch (Throwable th) {
            // ignore because the plain war does not see the OSGi API
        }

        String msg = req.getParameter("input");
        Writer writer = resp.getWriter();
        writer.write(msg + bundleSource);
    }
}
