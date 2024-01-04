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
@WebServlet(urlPatterns = "/InfoServlet")
public class InfoServlet extends HttpServlet {

    // sign whether LifecycleCallbackInterceptor's @PreDestroy method intercepting RemoteServlet was invoked
    private volatile int preDestroyInvocations;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String event = request.getParameter("event");

        if ("preDestroyNotify".equals(event)) {
            preDestroyInvocations++;
        } else if ("preDestroyVerify".equals(event)) {
            response.getWriter().append("" + preDestroyInvocations);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
