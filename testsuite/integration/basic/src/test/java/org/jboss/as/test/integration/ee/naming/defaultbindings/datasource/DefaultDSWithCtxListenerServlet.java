/*
 * JBoss, Home of Professional Open Source
 * Copyright 2020, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.ee.naming.defaultbindings.datasource;

import org.junit.Assert;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
