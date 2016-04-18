/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet invoking toString() method on class loader of class supplied as request parameter
 */
@WebServlet(urlPatterns = EjbInvokeClassloaderToStringServlet.URL_PATTERN)
public class EjbInvokeClassloaderToStringServlet extends HttpServlet {

    public static final String URL_PATTERN = "/invokeToStringOnClassloaderOfClass";

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
            Method invokeToStringOnClassloaderOfClassClassMethod;
            invokeToStringOnClassloaderOfClassClassMethod = classInJar.getMethod("invokeToStringOnClassloaderOfClass", String.class);
            resp.getOutputStream().print(invokeToStringOnClassloaderOfClassClassMethod.invoke(classInJar.newInstance(), className).toString());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                SecurityException | IllegalArgumentException | InvocationTargetException ex) {
            resp.getOutputStream().print(ex.toString());
        }
    }
}
