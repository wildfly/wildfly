/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
