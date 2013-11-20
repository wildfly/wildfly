/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ee.injection.support.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
            assertEquals("Listener field not injected", "true", req.getAttribute("field.injected").toString());
        } else if ("method".equals(mode)) {
            assertEquals("Listener setter not injected", "true", req.getAttribute("setter.injected").toString());
        } else if ("constructor".equals(mode)) {
            assertTrue("Listener constructor not injected", getNameFromRequest(req).contains("Joe"));
        } else if ("interceptorReset".equals(mode)) {
            ComponentInterceptor.resetInterceptions();
            assertEquals(0, ComponentInterceptor.getInterceptions().size());
            resp.getWriter().append("" + ComponentInterceptor.getInterceptions().size());
        } else if ("aroundInvokeVerify".equals(mode)) {
            assertEquals("Listener invocation not intercepted", 2, ComponentInterceptor.getInterceptions().size());
            assertEquals("requestDestroyed", ComponentInterceptor.getInterceptions().get(0).getMethodName());
            assertEquals("requestInitialized", ComponentInterceptor.getInterceptions().get(1).getMethodName());
            resp.getWriter().append("" + ComponentInterceptor.getInterceptions().size());
        } else if ("aroundConstructVerify".equals(mode)) {
            assertTrue("AroundConstruct interceptor method not invoked", AroundConstructInterceptor.aroundConstructCalled);
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
