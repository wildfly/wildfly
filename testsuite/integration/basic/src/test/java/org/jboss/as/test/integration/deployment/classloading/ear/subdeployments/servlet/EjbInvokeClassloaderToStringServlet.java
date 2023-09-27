/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
