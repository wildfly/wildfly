/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Servlet invoking toString() method on class supplied as request parameter
 */
@WebServlet(urlPatterns = EjbInvokeToStringServlet.URL_PATTERN)
public class EjbInvokeToStringServlet extends HttpServlet {

    public static final String URL_PATTERN = "/ejbinvoketostringservlet";

    public static final String CLASS_NAME_PARAMETER = "className";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String className = req.getParameter(CLASS_NAME_PARAMETER);
        if (className == null) {
            throw new ServletException(CLASS_NAME_PARAMETER + " parameter not set in request");
        }
        try {
            // Invoke toString() method on className
            Class<?> classInJar = Class
                    .forName("org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.ClassInJar");
            Method invokeToStringOnClassMethod;
            invokeToStringOnClassMethod = classInJar.getMethod("invokeToStringOnInstance", String.class);
            resp.getOutputStream().print(invokeToStringOnClassMethod.invoke(classInJar.newInstance(), className).toString());
        } catch (ClassNotFoundException cnfe) {
            resp.getOutputStream().print(cnfe.toString());
        } catch (InstantiationException ie) {
            resp.getOutputStream().print(ie.toString());
        } catch (IllegalAccessException iae) {
            resp.getOutputStream().print(iae.toString());
        } catch (NoSuchMethodException nsme) {
            resp.getOutputStream().print(nsme.toString());
        } catch (SecurityException se) {
            resp.getOutputStream().print(se.toString());
        } catch (IllegalArgumentException iae) {
            resp.getOutputStream().print(iae.toString());
        } catch (InvocationTargetException ite) {
            resp.getOutputStream().print(ite.toString());
        }
    }
}
