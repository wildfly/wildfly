/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.as.test.integration.ee.injection.support.AroundConstructInterceptor;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptor;

@SuppressWarnings("serial")
@WebServlet("/TestListenerServlet")
public class TestListenerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String mode = req.getParameter("mode");
        resp.setContentType("text/plain");

        if ("field".equals(mode)) {
            assertEquals("true", req.getAttribute("field.injected").toString(), "Listener field not injected");
        } else if ("method".equals(mode)) {
            assertEquals("true", req.getAttribute("setter.injected").toString(), "Listener setter not injected");
        } else if ("constructor".equals(mode)) {
            assertTrue(getNameFromRequest(req).contains("Joe"), "Listener constructor not injected");
        } else if ("interceptorReset".equals(mode)) {
            ComponentInterceptor.resetInterceptions();
            assertEquals(0, ComponentInterceptor.getInterceptions().size());
            resp.getWriter().append("" + ComponentInterceptor.getInterceptions().size());
        } else if ("aroundInvokeVerify".equals(mode)) {
            assertEquals(2, ComponentInterceptor.getInterceptions().size(), "Listener invocation not intercepted");
            assertEquals("requestDestroyed", ComponentInterceptor.getInterceptions().get(0).getMethodName());
            assertEquals("requestInitialized", ComponentInterceptor.getInterceptions().get(1).getMethodName());
            resp.getWriter().append("" + ComponentInterceptor.getInterceptions().size());
        } else if ("aroundConstructVerify".equals(mode)) {
            assertTrue(AroundConstructInterceptor.aroundConstructCalled, "AroundConstruct interceptor method not invoked");
            String name = getNameFromRequest(req);
            assertNotNull(name);
            assertTrue(name.contains("AroundConstructInterceptor#"));
            resp.getWriter().append(name);
        } else {
            resp.setStatus(404);
        }
    }

    private static String getNameFromRequest(HttpServletRequest req) {
        return req.getAttribute("name").toString();
    }
}
