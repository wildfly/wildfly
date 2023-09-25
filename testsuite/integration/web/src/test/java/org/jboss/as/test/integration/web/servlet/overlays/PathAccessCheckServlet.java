/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.servlet.overlays;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author  Jaikiran Pai
 */
@WebServlet(name = "PathAccessCheckServlet", urlPatterns = {"/check-path-access"})
public class PathAccessCheckServlet extends HttpServlet {

    static final String ACCESS_CHECKS_CORRECTLY_VALIDATED = "access-checks-valid";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        final String path = req.getParameter("path");
        final String shouldBeAccessible = req.getParameter("expected-accessible");
        final boolean expectedAccessible = shouldBeAccessible == null ? false : Boolean.parseBoolean(shouldBeAccessible);
        try (InputStream is = req.getServletContext().getResourceAsStream(path)) {
            if (expectedAccessible && is == null) {
                throw new ServletException("Expected to be accessible but could not access " + path);
            }
            if (!expectedAccessible && is != null) {
                throw new ServletException("Expected to be inaccessible but could access " + path);
            }
        }
        resp.getWriter().write(ACCESS_CHECKS_CORRECTLY_VALIDATED);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        this.doGet(req, resp);
    }
}
