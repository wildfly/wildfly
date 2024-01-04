/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.naming.defaultbindings.datasource;

import jakarta.annotation.Resource;
import javax.naming.InitialContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;

/**
 * @author Eduardo Martins
 */
@WebServlet(name = "SimpleServlet", urlPatterns = { "/simple" })
public class DefaultDataSourceTestServlet extends HttpServlet {

    @Resource
    private DataSource injectedResource;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // check injected resource
            if(injectedResource == null) {
                throw new NullPointerException("injected resource");
            }
            // checked jndi lookup
            new InitialContext().lookup("java:comp/DefaultDataSource");
        } catch (Throwable e) {
            throw new ServletException(e);
        }
    }

}
