/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.naming.defaultbindings.concurrency;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.InitialContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Eduardo Martins
 */
@WebServlet(name = "SimpleServlet", urlPatterns = { "/simple" })
public class DefaultConcurrencyTestServlet extends HttpServlet {

    @Resource
    private ContextService contextService;

    @Resource
    private ManagedExecutorService managedExecutorService;

    @Resource
    private ManagedScheduledExecutorService managedScheduledExecutorService;

    @Resource
    private ManagedThreadFactory managedThreadFactory;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // check injected resources
            if(contextService == null) {
                throw new NullPointerException("contextService");
            }
            if(managedExecutorService == null) {
                throw new NullPointerException("managedExecutorService");
            }
            if(managedScheduledExecutorService == null) {
                throw new NullPointerException("managedScheduledExecutorService");
            }
            if(managedThreadFactory == null) {
                throw new NullPointerException("managedThreadFactory");
            }
            // checked jndi lookup
            new InitialContext().lookup("java:comp/DefaultContextService");
            new InitialContext().lookup("java:comp/DefaultManagedExecutorService");
            new InitialContext().lookup("java:comp/DefaultManagedScheduledExecutorService");
            new InitialContext().lookup("java:comp/DefaultManagedThreadFactory");
        } catch (Throwable e) {
            throw new ServletException(e);
        }
    }

}
