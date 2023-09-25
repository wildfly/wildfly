/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.naming.defaultbindings.datasource;

import org.junit.Assert;

import jakarta.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * This can be used only in tests which remove default datasource binding from ee subsystem.
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@WebServlet(name = "DefaultDSWithCtxListenerServlet", urlPatterns = {"/defaultDSWithCtxListener"}, loadOnStartup = 1)
public class DefaultDSWithCtxListenerServlet extends HttpServlet {

    @Resource(name = "ds")
    private DataSource dataSource;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();
        // this is expected to be null because default datasource binding was removed from ee subsystem
        Assert.assertNull(dataSource);
        // the ds injection can be done in a ServeltContextListener just like what spring does
        Assert.assertNotNull(getServletContext().getAttribute("lookupDS"));
        // but it looks like java:comp/env/ds does not work
        try {
            new InitialContext().lookup("java:comp/env/ds");
            Assert.fail("java:comp/env/ds should not be available ??");
        } catch (NamingException e) {
            Assert.assertTrue(e.getMessage().contains("env/ds"));
        }
        out.print("OK");
        out.flush();
    }

    // this simulates spring context which does resource injection.
    @WebListener
    public static class CtxListener implements ServletContextListener {
        @Override
        public void contextInitialized(ServletContextEvent sce) {
            try {
                Object obj = new InitialContext().lookup("java:jboss/datasources/ExampleDS");
                sce.getServletContext().setAttribute("lookupDS", obj);
            } catch (NamingException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

}
