/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.sasl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.sasl.SaslException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.common.jms.ActiveMQProviderJMSOperations;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

/**
 * Abstract parent for JMS client based SASL mechanisms tests.
 *
 * @author Josef Cacek
 */
public abstract class AbstractSaslTestBase {

    private static Logger LOGGER = Logger.getLogger(AbstractSaslTestBase.class);

    protected static final String NAME = AbstractSaslTestBase.class.getSimpleName();
    protected static final String JNDI_QUEUE_NAME = "java:jboss/exported/" + NAME;

    protected static final String MESSAGE = "Hello, World!";
    protected static final String CONNECTION_FACTORY = "jms/RemoteConnectionFactory";

    protected static final String HOST = Utils.getDefaultHost(false);
    protected static final String HOST_FMT = NetworkUtils.formatPossibleIpv6Address(HOST);

    @Deployment(testable = false)
    public static WebArchive dummyDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war").addAsWebResource(new StringAsset("Test"), "index.html");
    }

    protected void sendAndReceiveMsg(int remotingPort, boolean expectedSaslFail) {
        sendAndReceiveMsg(remotingPort, expectedSaslFail, null, null);
    }

    protected void sendAndReceiveMsg(int remotingPort, boolean expectedSaslFail, String username, String password) {
        Context namingContext = null;

        try {
            // Set up the namingContext for the JNDI lookup
            final Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
            env.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
            env.put(Context.PROVIDER_URL, "remote://" + HOST_FMT + ":" + remotingPort);
            namingContext = new InitialContext(env);

            // Perform the JNDI lookups
            ConnectionFactory connectionFactory = null;
            try {
                connectionFactory = (ConnectionFactory) namingContext.lookup(CONNECTION_FACTORY);
                assertFalse("JNDI lookup should have failed.", expectedSaslFail);
            } catch (NamingException e) {
                if (expectedSaslFail) {
                    // only SASL failures are expected
                    assertTrue("Unexpected cause of lookup failure", e.getCause() instanceof SaslException);
                    return;
                }
                throw e;
            }

            Destination destination = (Destination) namingContext.lookup(NAME);

            try (JMSContext context = (username != null ? connectionFactory.createContext(username, password)
                    : connectionFactory.createContext())) {
                // Send a message
                context.createProducer().send(destination, MESSAGE);

                // Create the JMS consumer
                JMSConsumer consumer = context.createConsumer(destination);
                // Then receive the same message that was sent
                String text = consumer.receiveBody(String.class, 5000);
                Assert.assertEquals(MESSAGE, text);
            }
        } catch (NamingException e) {
            LOGGER.error("Naming problem occured.", e);
            throw new RuntimeException(e);
        } finally {
            if (namingContext != null) {
                try {
                    namingContext.close();
                } catch (NamingException e) {
                    LOGGER.error("Naming problem occured during closing context.", e);
                }
            }
        }
    }

    /**
     * Setup task which configures test queue in the messaging subsystem.
     */
    public static class JmsSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            new ActiveMQProviderJMSOperations(managementClient).createJmsQueue(NAME, JNDI_QUEUE_NAME);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            new ActiveMQProviderJMSOperations(managementClient).removeJmsQueue(NAME);
        }

    }

  }
