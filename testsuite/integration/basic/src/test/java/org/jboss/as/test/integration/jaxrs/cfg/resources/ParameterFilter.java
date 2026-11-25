/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.cfg.resources;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@WebFilter("/*")
public class ParameterFilter implements Filter {

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        // Simple consume the parameters to trigger the input stream to be read.
        final Map<String, String[]> params = request.getParameterMap();
        chain.doFilter(request, response);
    }
}
