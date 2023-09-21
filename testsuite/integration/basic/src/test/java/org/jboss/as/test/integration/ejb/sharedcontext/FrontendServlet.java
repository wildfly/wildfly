/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ejb.sharedcontext;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Servlet shares the context for the client servlets
 */
@WebServlet(urlPatterns = "/*")
public class FrontendServlet extends HttpServlet {

    private static final int MAX_CLIENTS = 50;
    public static final String FRONTEND_SERVLET_OK = "OK";
    private static final String FRONTEND_SERVLET_FAILED = "FAILED";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        final PrintWriter out = resp.getWriter();

        try {
            for (int i = 0; i < MAX_CLIENTS; i++) {
                String contextPath = String.format("/shared-client-context-client-%02d", i);
                ServletContext context = req.getServletContext().getContext(contextPath);
                if (context == null || !context.getContextPath().equals(contextPath)) {
                    break;
                }
                RequestDispatcher disp = context.getRequestDispatcher("/");
                disp.include(req, resp);
            }
            out.print(FrontendServlet.FRONTEND_SERVLET_OK);
        } catch (Exception e) {
            e.printStackTrace();
            out.print(FrontendServlet.FRONTEND_SERVLET_FAILED);
        }
    }
}
