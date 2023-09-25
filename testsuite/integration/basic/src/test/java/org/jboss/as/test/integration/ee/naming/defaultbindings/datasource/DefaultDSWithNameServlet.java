/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.naming.defaultbindings.datasource;

import org.junit.Assert;

import jakarta.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
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
public class DefaultDSWithNameServlet extends HttpServlet {

    private boolean hasLookup;

    @Resource(name = "ds")
    private DataSource dataSource;

    @Override
    public void init() throws ServletException {
        try {
            hasLookup = Boolean.parseBoolean(getServletConfig().getInitParameter("hasLookup"));
        } catch (Exception e) {
            hasLookup = false;
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();
        if (hasLookup) {
            if (dataSource == null) {
                throw new ServletException("dataSource is null when lookup is defined in jboss-web.xml");
            }
        } else {
            if (dataSource != null) {
                throw new ServletException("No lookup is defined, dataSource should be null");
            }
        }

        if (hasLookup) {
            try {
                Assert.assertNotNull(new InitialContext().lookup("java:comp/env/ds"));
            } catch (NamingException e) {
                throw new IOException("Cannot lookup java:comp/env/ds when has lookup specified", e);
            }
        } else {
            try {
                new InitialContext().lookup("java:comp/env/ds");
                Assert.fail("lookup should fail when no lookup specified for 'java:comp/env/ds'");
            } catch (NamingException e) {
                Assert.assertTrue(e.getMessage().contains("env/ds"));
            }
        }
        out.print("OK");
        out.flush();
    }
}
