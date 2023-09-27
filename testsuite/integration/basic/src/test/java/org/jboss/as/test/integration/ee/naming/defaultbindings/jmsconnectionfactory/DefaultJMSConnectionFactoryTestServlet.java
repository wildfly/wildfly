/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.naming.defaultbindings.jmsconnectionfactory;

import jakarta.annotation.Resource;
import jakarta.jms.ConnectionFactory;
import javax.naming.InitialContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Eduardo Martins
 */
@WebServlet(name = "SimpleServlet", urlPatterns = { "/simple" })
public class DefaultJMSConnectionFactoryTestServlet extends HttpServlet {

    @Resource
    private ConnectionFactory injectedResource;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // check injected resource
            if(injectedResource == null) {
                throw new NullPointerException("injected resource");
            }
            // checked jndi lookup
            new InitialContext().lookup("java:comp/DefaultJMSConnectionFactory");
        } catch (Throwable e) {
            throw new ServletException(e);
        }
    }

}
