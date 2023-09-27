/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.concurrent;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
