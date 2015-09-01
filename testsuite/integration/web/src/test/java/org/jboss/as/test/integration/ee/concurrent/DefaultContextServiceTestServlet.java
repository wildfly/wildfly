/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ee.concurrent;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ContextService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Principal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Eduardo Martins
 */
@WebServlet(name = "SimpleServlet", urlPatterns = { "/simple" })
public class DefaultContextServiceTestServlet extends HttpServlet {

    @Resource
    private ContextService contextService;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.login("guest","guest");
        Principal principal = req.getUserPrincipal();
        String moduleName = null;
        try {
            moduleName = (String) new InitialContext().lookup("java:module/ModuleName");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
        final Runnable runnable = new TestServletRunnable(moduleName);
        final Runnable contextualProxy = contextService.createContextualProxy(runnable,Runnable.class);
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            // WFLY-4308: test serialization of contextual proxies
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final ObjectOutputStream stream = new ObjectOutputStream(bytes);
            stream.writeObject(contextualProxy);
            stream.close();
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                executorService.submit((Runnable) in.readObject()).get();
            }
        } catch (Throwable e) {
            throw new ServletException(e);
        } finally {
            executorService.shutdown();
        }
    }

}
