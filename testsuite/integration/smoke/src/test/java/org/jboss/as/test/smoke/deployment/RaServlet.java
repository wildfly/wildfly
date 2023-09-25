/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.deployment;

import java.io.IOException;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.as.test.smoke.deployment.rar.MultipleAdminObject1;
import org.jboss.as.test.smoke.deployment.rar.MultipleConnectionFactory1;

/**
 * User: Jaikiran Pai
 */
@WebServlet(name = "RaServlet", urlPatterns = RaServlet.URL_PATTERN)
public class RaServlet extends HttpServlet {

    public static final String SUCCESS = "SUCCESS";
    public static final String URL_PATTERN = "/raservlet";

    @Resource(name = "java:jboss/name1")
    private MultipleConnectionFactory1 connectionFactory1;


    @Resource(name = "java:jboss/Name3")
    private MultipleAdminObject1 adminObject1;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuffer sb = new StringBuffer();
        if (connectionFactory1 == null) sb.append("CF1 is null.");
        if (adminObject1 == null) sb.append("AO1 is null.");
        resp.getOutputStream().print((sb.length() > 0) ? sb.toString() : SUCCESS);
    }
}
