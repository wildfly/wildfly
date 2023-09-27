/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.reverseproxy;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 */
@WebServlet(name = "ServerNameServlet", urlPatterns = {"/name"})
public class ServerNameServlet extends HttpServlet {

    private String message;

    @Override
    public void init(ServletConfig config) throws ServletException {
        message = config.getInitParameter("message");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if("true".equals(req.getParameter("session"))) {
            req.getSession(true);
        }
        if (req.getParameter("wait") != null) {
            try {
                Thread.sleep(Long.parseLong(req.getParameter("wait")));
            } catch (InterruptedException e) {}
        }
        resp.getWriter().write(message);
        resp.getWriter().close();
    }


}
