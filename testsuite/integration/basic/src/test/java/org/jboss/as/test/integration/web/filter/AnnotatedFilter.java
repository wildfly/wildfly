/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.web.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;

/**
 * @author Tomas Hofman (thofman@redhat.com)
 */
@WebFilter(value = "/*", description = "Annotated filter")
public class AnnotatedFilter implements Filter {

    public static final String OUTPUT = "filter";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        servletResponse.getOutputStream().write(OUTPUT.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void destroy() {
    }
}
