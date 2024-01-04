/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.localtransaction;

import java.io.IOException;

import jakarta.annotation.Resource;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConnectionFactoryDefinition;
import jakarta.jms.JMSException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Assert;


/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
@JMSConnectionFactoryDefinition(
        name="java:module/CF_allow_local_tx",
        properties = {
                "connectors=${org.jboss.messaging.default-connector:in-vm}",
                "allow-local-transactions=true"
        }
)
@WebServlet("/LocalTransactionTestCase")
public class MessagingServlet extends HttpServlet {

    @Resource(lookup = "java:module/CF_allow_local_tx")
    private ConnectionFactory cFwithLocalTransaction;

    @Resource
    private ConnectionFactory defaultCF;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean allowLocalTransactions = Boolean.valueOf(req.getParameter("allowLocalTransactions"));

        final ConnectionFactory cf = allowLocalTransactions ? cFwithLocalTransaction : defaultCF;
        Connection connection = null;
        try {
            connection = cf.createConnection();
            try {
                connection.createSession(true, 0);
                if (!allowLocalTransactions) {
                    Assert.fail("Local transactions are not allowed");
                }
                resp.getWriter().write("" + allowLocalTransactions);
            } catch (JMSException e) {
                if (allowLocalTransactions) {
                    Assert.fail("Local transactions are allowed");
                }
                resp.getWriter().write("" + allowLocalTransactions);
            }
        } catch (Throwable t) {
            resp.setStatus(500);
            t.printStackTrace(resp.getWriter());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                }
            }
        }
    }
}
