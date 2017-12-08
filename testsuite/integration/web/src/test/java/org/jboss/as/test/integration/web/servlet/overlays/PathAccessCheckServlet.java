/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
 *
 */

package org.jboss.as.test.integration.web.servlet.overlays;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
