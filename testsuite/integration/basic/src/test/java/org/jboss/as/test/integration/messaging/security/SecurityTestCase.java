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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.security.CheckType;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
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
        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getMgmtAddress(), managementClient.getWebUri().getPort());
        try {
            sf.createSession("fail", "epicfail", false, true, true, false, 1);
            fail("must not allow to create a session with bad authentication");
        } catch (ActiveMQException e) {
            assertEquals(ActiveMQExceptionType.SECURITY_EXCEPTION, e.getType());
            assertTrue(e.getMessage(), e.getMessage().startsWith("AMQ119031"));
        } finally {
            if (sf != null) {
                sf.close();
            }
        }
    }

    @Test
    public void testFailedAuthenticationBlankUserPass() throws Exception {
        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getMgmtAddress(), managementClient.getWebUri().getPort());
        try {
            sf.createSession();
            fail("must not allow to create a session without any authentication");
        } catch (ActiveMQException e) {
            assertEquals(ActiveMQExceptionType.SECURITY_EXCEPTION, e.getType());
            assertTrue(e.getMessage(), e.getMessage().startsWith("AMQ119031"));
        } finally {
            if (sf != null) {
                sf.close();
            }
        }
    }

    @Test
    public void testDefaultClusterUser() throws Exception {
        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getMgmtAddress(), managementClient.getWebUri().getPort());
        try {
            sf.createSession(ActiveMQDefaultConfiguration.getDefaultClusterUser(), ActiveMQDefaultConfiguration.getDefaultClusterPassword(), false, true, true, false, 1);
            fail("must not allow to create a session with the default cluster user credentials");
        } catch (ActiveMQException e) {
            assertEquals(ActiveMQExceptionType.CLUSTER_SECURITY_EXCEPTION, e.getType());
            assertTrue(e.getMessage(), e.getMessage().startsWith("AMQ119099"));
        } finally {
            if (sf != null) {
                sf.close();
            }
        }
    }

    @Test
    public void testSuccessfulAuthentication() throws Exception {
        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getMgmtAddress(), managementClient.getWebUri().getPort());
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

        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getMgmtAddress(), managementClient.getWebUri().getPort());

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
        final ClientSessionFactory sf = createClientSessionFactory(managementClient.getMgmtAddress(), managementClient.getWebUri().getPort());
        ClientSession session = null;
        try {
            session = sf.createSession("guest", "guest", false, true, true, false, 1);
            session.createQueue(queueName, queueName, true);
            fail("Must not create a durable queue without the CREATE_DURABLE_QUEUE permission");
        } catch (ActiveMQException e) {
            assertEquals(ActiveMQExceptionType.SECURITY_EXCEPTION, e.getType());
            // Code of exception has changed in Artemis 2.x
            assertTrue(e.getMessage().startsWith("AMQ119213"));
            assertTrue(e.getMessage().contains(CheckType.CREATE_DURABLE_QUEUE.toString()));
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    static ClientSessionFactory createClientSessionFactory(String host, int port) throws Exception {
        final Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("host", host);
        properties.put("port", port);
        properties.put(TransportConstants.HTTP_UPGRADE_ENABLED_PROP_NAME, true);
        properties.put(TransportConstants.HTTP_UPGRADE_ENDPOINT_PROP_NAME, "http-acceptor");
        final TransportConfiguration configuration = new TransportConfiguration(NettyConnectorFactory.class.getName(), properties);
        return ActiveMQClient.createServerLocatorWithoutHA(configuration).createSessionFactory();
    }
}
