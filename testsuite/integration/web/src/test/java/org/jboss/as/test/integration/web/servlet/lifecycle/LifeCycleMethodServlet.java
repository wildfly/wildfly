/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.servlet.lifecycle;

import java.io.IOException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 */
@WebServlet(name = "LifeCycleMethodServlet", urlPatterns = {"/LifeCycleMethodServlet"})
public class LifeCycleMethodServlet extends HttpServlet implements ServletContextListener {

    String message;

    public void postConstruct() {
        message = "ok";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().write(message);
        resp.getWriter().close();
    }

    // test AS7-5746 use case

    private static final String NAME = "java:global/env/foo";
    private static final String VALUE = "FOO";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Context context = new InitialContext();
            context.bind(NAME, VALUE);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            Context context = new InitialContext();
            context.lookup(NAME);
            context.unbind(NAME);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
