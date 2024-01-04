/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.sharedcontext;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

@WebServlet(urlPatterns = "/*")
public class EjbClientServlet extends HttpServlet {
    private static final String INITIAL_CONTEXT_FACTORY = "org.wildfly.naming.client.WildFlyInitialContextFactory";
    private static final String MODULE_NAME = "shared-client-context-ejb";
    private static final String PROVIDER_URL = "remote+http://localhost:8080";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final PrintWriter out = resp.getWriter();
        InitialContext ctx = null;
        try {
            Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
            props.put(Context.PROVIDER_URL, PROVIDER_URL);
            ctx = new InitialContext(props);

            TestEjbRemote myEjb = (TestEjbRemote) ctx.lookup("ejb:/" + MODULE_NAME + "/" + "TestEjb!" + TestEjbRemote.class.getName());
            myEjb.test();
        } catch (NamingException e) {
            e.printStackTrace(out);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
