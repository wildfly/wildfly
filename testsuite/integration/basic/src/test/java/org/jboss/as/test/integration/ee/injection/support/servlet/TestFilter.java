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

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.test.integration.ee.injection.support.Alpha;
import org.jboss.as.test.integration.ee.injection.support.Bravo;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptor;
import org.jboss.as.test.integration.ee.injection.support.ComponentInterceptorBinding;

@ComponentInterceptorBinding
@WebFilter("/TestFilter")
public class TestFilter implements Filter {

    @Inject
    private Alpha alpha;

    private Bravo bravo;

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
        } else if ("interceptorReset".equals(mode)) {
            // Reset the interceptions - it will be incremented at the end of the service() method invocation
            ComponentInterceptor.resetInterceptions();
            assertEquals(0, ComponentInterceptor.getInterceptions().size());
            resp.getWriter().append("" + ComponentInterceptor.getInterceptions().size());
        } else if ("interceptorVerify".equals(mode)) {
            assertEquals("Filter invocation not intercepted", 1, ComponentInterceptor.getInterceptions().size());
            assertEquals("doFilter", ComponentInterceptor.getInterceptions().get(0).getMethodName());
            resp.getWriter().append("" + ComponentInterceptor.getInterceptions().size());
        } else {
            resp.setStatus(404);
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }
}
