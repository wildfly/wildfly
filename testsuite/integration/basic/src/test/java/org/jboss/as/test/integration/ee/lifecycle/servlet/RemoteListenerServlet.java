/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.lifecycle.servlet;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet("/RemoteListenerServlet")
public class RemoteListenerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String event = req.getParameter("event");
        resp.setContentType("text/plain");

        if ("postConstructVerify".equals(event)) {
            resp.setContentType("text/plain");
            resp.getWriter().append("" + LifecycleCallbackInterceptor.getPostConstructIncations());
        } else {
            resp.setStatus(404);
        }
    }
}
