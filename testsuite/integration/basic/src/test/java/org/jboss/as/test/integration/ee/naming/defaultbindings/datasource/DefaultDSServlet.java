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
@WebServlet(name = "DefaultDSServlet", urlPatterns = {"/defaultDS"}, loadOnStartup = 1)
public class DefaultDSServlet extends HttpServlet {

    @Resource()
    private DataSource dataSource;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter out = resp.getWriter();
        // this is expected to be null because default datasource binding was removed from ee subsystem
        Assert.assertNull(dataSource);
        try {
            new InitialContext().lookup("java:comp/env/dataSource");
            Assert.fail("Lookup should fail!");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NamingException);
        }
        out.print("OK");
        out.flush();
    }
}
