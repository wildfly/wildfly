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
import javax.servlet.ServletException;
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
