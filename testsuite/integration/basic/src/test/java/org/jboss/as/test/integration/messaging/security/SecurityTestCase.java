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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.eclipse.jdt.internal.compiler.ast.AssertStatement;
import org.hornetq.api.config.HornetQDefaultConfiguration;
import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.HornetQExceptionType;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.ClientConsumer;
import org.hornetq.api.core.client.ClientSession;
import org.hornetq.api.core.client.ClientSessionFactory;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.config.HornetQDefaultConfiguration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.security.CheckType;
import org.hornetq.core.security.Role;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the security integration for HornetQ
 *
 * @author Justin Bertram (c) 2011 Red Hat Inc.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class SecurityTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testFailedAuthenticationBadUserPass() throws Exception {
        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getMgmtAddress(), 5445);
        try {
            sf.createSession("fail", "epicfail", false, true, true, false, 1);
            fail("must not allow to create a session with bad authentication");
        } catch (HornetQException e) {
            assertEquals(HornetQExceptionType.SECURITY_EXCEPTION, e.getType());
            assertTrue(e.getMessage().startsWith("HQ119061"));
        } finally {
            if (sf != null) {
                sf.close();
            }
        }
    }

    @Test
    public void testFailedAuthenticationBlankUserPass() throws Exception {
        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getMgmtAddress(), 5445);
        try {
            sf.createSession();
            fail("must not allow to create a session without any authentication");
        } catch (HornetQException e) {
            assertEquals(HornetQExceptionType.SECURITY_EXCEPTION, e.getType());
            assertTrue(e.getMessage().startsWith("HQ119061"));
        } finally {
            if (sf != null) {
                sf.close();
            }
        }
    }

    @Test
    public void testDefaultClusterUser() throws Exception {
        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getMgmtAddress(), 5445);
        try {
            sf.createSession(HornetQDefaultConfiguration.DEFAULT_CLUSTER_USER, HornetQDefaultConfiguration.DEFAULT_CLUSTER_PASSWORD, false, true, true, false, 1);
            fail("must not allow to create a session with the default cluster user credentials");
        } catch (HornetQException e) {
            assertEquals(HornetQExceptionType.SECURITY_EXCEPTION, e.getType());
            assertTrue(e.getMessage().startsWith("HQ119061"));
        } finally {
            if (sf != null) {
                sf.close();
            }
        }
    }

    @Test
    public void testSuccessfulAuthentication() throws Exception {
        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getMgmtAddress(), 5445);
        ClientSession session = null;
        try {
            session = sf.createSession("guest", "guest", false, true, true, false, 1);
            assertNotNull(session);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @Test
    public void testSuccessfulAuthorization() throws Exception {
        final String queueName = "queue.testSuccessfulAuthorization";

        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getMgmtAddress(), 5445);

        ClientSession session = null;
        try {
            session = sf.createSession("guest", "guest", false, true, true, false, 1);
            session.createQueue(queueName, queueName, false);
            ClientConsumer messageConsumer = session.createConsumer(queueName);
            session.start();
            session.stop();
            messageConsumer.close();
            session.deleteQueue(queueName);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }


    @Test
    public void testUnsuccessfulAuthorization() throws Exception {
        final String queueName = "queue.testUnsuccessfulAuthorization";
        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getMgmtAddress(), 5445);
        ClientSession session = null;
        try {
            session = sf.createSession("guest", "guest", false, true, true, false, 1);
            session.createQueue(queueName, queueName, true);
            fail("Must not create a durable queue without the CREATE_DURABLE_QUEUE permission");
        } catch (HornetQException e) {
            assertEquals(HornetQExceptionType.SECURITY_EXCEPTION, e.getType());
            assertTrue(e.getMessage().startsWith("HQ119062"));
            assertTrue(e.getMessage().contains(CheckType.CREATE_DURABLE_QUEUE.toString()));
        } finally {
            if (session != null) {
                session.close();
            }
        }
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
