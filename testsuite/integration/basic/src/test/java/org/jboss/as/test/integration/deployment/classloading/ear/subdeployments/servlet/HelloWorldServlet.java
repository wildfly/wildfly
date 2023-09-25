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
 * User: Jaikiran Pai
 */
@WebServlet(name = "HelloWorldServlet", urlPatterns = HelloWorldServlet.URL_PATTERN)
public class HelloWorldServlet extends HttpServlet {

    public static final String URL_PATTERN = "/helloworld";

    public static final String PARAMETER_NAME = "message";
    @EJB
    private EJBBusinessInterface echoBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String message = req.getParameter(PARAMETER_NAME);
        if (message == null) {
            throw new ServletException(PARAMETER_NAME + " parameter not set in request");
        }
        final String echo = this.echoBean.echo(message);
        // print out the echo message
        resp.getOutputStream().print(echo);
    }
}
