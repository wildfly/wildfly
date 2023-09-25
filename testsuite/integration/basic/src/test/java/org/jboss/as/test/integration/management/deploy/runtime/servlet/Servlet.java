/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.deploy.runtime.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Simplistic servlet
 * @author baranowb
 *
 */
@WebServlet (urlPatterns = Servlet.URL_PATTERN)
public class Servlet extends HttpServlet {
    public static final String URL_PATTERN ="/runny-nose";
    public static String SUCCESS="minion";
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getOutputStream().write(SUCCESS.getBytes(StandardCharsets.UTF_8));
    }
}
