/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.security;

import junit.framework.Assert;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSession.QueueQuery;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests the security integration for HornetQ
 *
 * @author Justin Bertram (c) 2011 Red Hat Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class SecurityTestCase {
    @BeforeClass
    public static void setup() throws Exception {
    }

    @AfterClass
    public static void cleanup() throws Exception {
    }

    @Test
    public void testFailedAuthenticationBadUserPass() throws Exception {
        final ClientSessionFactory sf = createClientSessionFactory("localhost", 5445);
        ClientSession session = null;
        boolean success = false;
        try {
            session = sf.createSession("fail", "epicfail", false, true, true, false, 1);
        } catch (Exception e) {
            if ("Unable to validate user: fail".equals(e.getMessage())) {
                success = true;
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }

        Assert.assertTrue(success);
    }

    @Test
    public void testFailedAuthenticationBlankUserPass() throws Exception {
        final ClientSessionFactory sf = createClientSessionFactory("localhost", 5445);
        ClientSession session = null;
        boolean success = false;
        try {
            session = sf.createSession();
        } catch (Exception e) {
            if ("Unable to validate user: null".equals(e.getMessage())) {
                success = true;
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }

        Assert.assertTrue(success);
    }

    @Test
    public void testSuccessfulAuthentication() throws Exception {
        final ClientSessionFactory sf = createClientSessionFactory("localhost", 5445);
        ClientSession session = null;
        boolean success = false;
        try {
            session = sf.createSession("guest", "guest", false, true, true, false, 1);
            success = true;
        } finally {
            if (session != null) {
                session.close();
            }
        }

        Assert.assertTrue(success);
    }

    @Test
    public void testSuccessfulAuthorization() throws Exception {
        boolean success = false;
        final String queueName = "queue.testSuccessfulAuthorization";

        final ClientSessionFactory sf = createClientSessionFactory("localhost", 5445);

        ClientSession session = null;
        try {
            session = sf.createSession("guest", "guest", false, true, true, false, 1);
            session.createQueue(queueName, queueName, false);
            ClientConsumer messageConsumer = session.createConsumer(queueName);
            session.start();
            session.stop();
            messageConsumer.close();
            session.deleteQueue(queueName);
            success = true;
        } finally {
            if (session != null) {
                session.close();
            }
        }

        Assert.assertTrue(success);
    }


    @Test
    public void testUnsuccessfulAuthorization() throws Exception {
        boolean success = false;
        final String queueName = "queue.testUnsuccessfulAuthorization";

        final ClientSessionFactory sf = createClientSessionFactory("localhost", 5445);

        ClientSession session = null;
        try {
            session = sf.createSession("guest", "guest", false, true, true, false, 1);
            session.createQueue(queueName, queueName, true);
        } catch (Exception e) {
            if ("User: guest doesn't have permission='CREATE_DURABLE_QUEUE' on address queue.testUnsuccessfulAuthorization".equals(e.getMessage())) {
                success = true;
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }

        Assert.assertTrue(success);
    }

    static void applyUpdate(ModelNode update, final ModelControllerClient client) throws IOException {
        ModelNode result = client.execute(new OperationBuilder(update).build());
        if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
            if (result.hasDefined("result")) {
                System.out.println(result.get("result"));
            }
        } else if (result.hasDefined("failure-description")) {
            throw new RuntimeException(result.get("failure-description").toString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
        }
    }

    static ClientSessionFactory createClientSessionFactory(String host, int port) throws Exception {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("host", host);
        properties.put("port", port);
        final TransportConfiguration configuration = new TransportConfiguration(NettyConnectorFactory.class.getName(), properties);
        return HornetQClient.createServerLocatorWithoutHA(configuration).createSessionFactory();
    }
}
