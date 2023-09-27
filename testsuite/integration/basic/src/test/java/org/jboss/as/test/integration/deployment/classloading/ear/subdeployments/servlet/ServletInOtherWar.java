/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: jpai
 */
@WebServlet(name = "OtherServlet", urlPatterns = ServletInOtherWar.URL_PATTERN)
public class ServletInOtherWar extends HttpServlet {

    public static final String URL_PATTERN = "/otherservlet";

    public static final String CLASS_IN_OTHER_WAR_PARAMETER = "classInOtherWar";

    public static final String SUCCESS_MESSAGE = "Success";

    public static final String FAILURE_MESSAGE = "Failure";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String classInOtherWar = req.getParameter(CLASS_IN_OTHER_WAR_PARAMETER);
        if (classInOtherWar == null) {
            throw new ServletException(CLASS_IN_OTHER_WAR_PARAMETER + " parameter not set in request");
        }
        try {
            Class<?> klass = this.getClass().getClassLoader().loadClass(classInOtherWar);
            // class from one war wasn't expected to be visible to other war
            resp.getOutputStream().print(FAILURE_MESSAGE);
        } catch (ClassNotFoundException cnfe) {
            // the ClassNotFoundException is expected since class in one war isn't expected to be visible to
            // another war (even if it belongs to the same .ear)
            resp.getOutputStream().print(SUCCESS_MESSAGE);
        }


    }
}
