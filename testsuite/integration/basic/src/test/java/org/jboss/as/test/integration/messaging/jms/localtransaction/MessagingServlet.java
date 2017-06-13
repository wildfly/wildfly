/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.test.integration.messaging.jms.localtransaction;

import java.io.IOException;

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactoryDefinition;
import javax.jms.JMSException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;


/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
@JMSConnectionFactoryDefinition(
        name="java:module/CF_allow_local_tx",
        properties = {
                "connectors=in-vm",
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
