/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.servlet;

import org.jboss.as.test.integration.deployment.classloading.ear.subdeployments.ejb.EJBBusinessInterface;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * User: jpai
 */
@WebServlet(urlPatterns = EjbInvokingServlet.URL_PATTERN)
public class EjbInvokingServlet extends HttpServlet {

    public static final String URL_PATTERN = "/ejbinvokingservlet";

    public static final String CLASS_IN_WAR_PARAMETER = "classInWar";

    public static final String SUCCESS_MESSAGE = "Success";

    public static final String FAILURE_MESSAGE = "Failure";

    @EJB
    private EJBBusinessInterface bean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String classInWar = req.getParameter(CLASS_IN_WAR_PARAMETER);
        if (classInWar == null) {
            throw new ServletException(CLASS_IN_WAR_PARAMETER + " parameter not set in request");
        }
        try {
            bean.loadClass(classInWar);
            // .war class shouldn't have been visible to an EJB in a .jar
            resp.getOutputStream().print(FAILURE_MESSAGE);
        } catch (ClassNotFoundException cnfe) {
            // the ClassNotFoundException is expected since the class in the .war isn't expected to be visible to the
            // EJB in the .jar
            resp.getOutputStream().print(SUCCESS_MESSAGE);
        }
    }
}
