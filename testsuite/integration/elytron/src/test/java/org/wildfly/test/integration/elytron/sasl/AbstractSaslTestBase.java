/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.sasl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
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
import org.jboss.as.test.shared.TimeoutUtil;
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
                String text = consumer.receiveBody(String.class, TimeoutUtil.adjust(5000));
                Assert.assertEquals(MESSAGE, text);
            }
        } catch (NamingException e) {
            LOGGER.error("Naming problem occurred.", e);
            throw new RuntimeException(e);
        } finally {
            if (namingContext != null) {
                try {
                    namingContext.close();
                } catch (NamingException e) {
                    LOGGER.error("Naming problem occurred during closing context.", e);
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
