/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jboss.as.test.integration.ee.injection.support.Alpha;
import org.jboss.as.test.integration.ee.injection.support.AroundConstructBinding;
import org.jboss.as.test.integration.ee.injection.support.AroundConstructInterceptor;
import org.jboss.as.test.integration.ee.injection.support.Bravo;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptor;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptorBinding;
import org.jboss.as.test.integration.ee.injection.support.ProducedString;

@ComponentInterceptorBinding
@AroundConstructBinding
@WebFilter("/TestFilter")
public class TestFilter implements Filter {

    @Inject
    private Alpha alpha;

    private Bravo bravo;

    private String name;

    @Inject
    public TestFilter(@ProducedString String name) {
        this.name = name + "#TestFilter";
    }

    @Inject
    public void setBravo(Bravo bravo) {
        this.bravo = bravo;
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String mode = req.getParameter("mode");
        resp.setContentType("text/plain");

        if ("field".equals(mode)) {
            assertNotNull(alpha);
            resp.getWriter().append(alpha.getId());
        } else if ("method".equals(mode)) {
            assertNotNull(bravo);
            resp.getWriter().append(bravo.getAlphaId());
        } else if ("constructor".equals(mode)) {
            assertNotNull(name);
            assertTrue(name.contains("Joe"));
            resp.getWriter().append(name);
        } else if ("interceptorReset".equals(mode)) {
            // Reset the interceptions - it will be incremented at the end of the service() method invocation
            ComponentInterceptor.resetInterceptions();
            assertEquals(0, ComponentInterceptor.getInterceptions().size());
            resp.getWriter().append("" + ComponentInterceptor.getInterceptions().size());
        } else if ("aroundInvokeVerify".equals(mode)) {
            assertEquals("Filter invocation not intercepted", 1, ComponentInterceptor.getInterceptions().size());
            assertEquals("doFilter", ComponentInterceptor.getInterceptions().get(0).getMethodName());
            resp.getWriter().append("" + ComponentInterceptor.getInterceptions().size());
        } else if ("aroundConstructVerify".equals(mode)) {
            assertTrue("AroundConstruct interceptor method not invoked", AroundConstructInterceptor.aroundConstructCalled);
            assertNotNull(name);
            assertTrue(name.contains("AroundConstructInterceptor#"));
            resp.getWriter().append(name);
        } else {
            resp.setStatus(404);
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }
}
