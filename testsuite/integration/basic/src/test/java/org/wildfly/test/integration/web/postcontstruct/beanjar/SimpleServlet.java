/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.web.postcontstruct.beanjar;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SimpleServlet extends HttpServlet {
    protected AtomicInteger m_postCount = new AtomicInteger(0);

    @Resource(name = "envEntry", type = Integer.class)
    protected Integer m_cInteger;

    @Inject
    private SimpleBean bean;

    @PostConstruct
    protected void post() {
        m_postCount.incrementAndGet();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.getWriter().write(""+(m_postCount.get() + m_cInteger));
    }
}
