/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.classloading.transientdependencies;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Jaikiran Pai
 */
public class Servlet extends HttpServlet {

    static final String SUCCESS = "success";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String className = req.getParameter("className");
        try {
            final Class<?> klass = Thread.currentThread().getContextClassLoader().loadClass(className);
            resp.getOutputStream().print(SUCCESS);
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("Could not load a class: " + className + " which was expected to be available", cnfe);
        }
    }
}
