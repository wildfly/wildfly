/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.servlet.lifecycle;

import java.io.IOException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


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
