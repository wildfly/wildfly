/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.messaging;

import java.io.File;
import java.io.IOException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.helpers.Operations;

import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.Properties;
import java.util.stream.Stream;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.commons.lang3.RandomStringUtils;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.repository.PathUtil;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test to check that we can configure properly how many journal files are stored in the attic when artemis journal is corrupted.
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class CorruptedLogTestCase {

    private static final String DEFAULT_FULL_JBOSSAS = "default-full-jbossas";

    @ArquillianResource
    protected static ContainerController container;

    private ManagementClient managementClient;
    private int counter = 0;

    @Before
    public void setup() throws Exception {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        managementClient = createManagementClient();
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        setJournalSize(100L);
        jmsOperations.createJmsQueue("corrupted", "java:jboss/exported/" + "queue/corrupted");
        jmsOperations.close();
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        PathUtil.deleteRecursively(getAtticPath());
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
    }

    @After
    public void tearDown() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.removeJmsQueue("corrupted");
        managementClient.getControllerClient().execute(Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "journal-file-size"));
        managementClient.getControllerClient().execute(Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "journal-max-attic-files"));
        jmsOperations.close();
        managementClient.close();
        if(counter > 1) {
            container.stop(DEFAULT_FULL_JBOSSAS);
        }
        PathUtil.deleteRecursively(getAtticPath());
    }

    /**
     * Set the journal file size to 100k.
     * Send 100 messages of 20k.
     * Set the journal file size to 200k.
     * Restart the server.
     * Consume the messages.
     * Should have journal files in the attic.
     *
     * @throws javax.naming.NamingException
     * @throws java.io.IOException
     */
    @Test
    public void testCorruptedJournal() throws NamingException, IOException {
        counter++;
        InitialContext remoteContext = createJNDIContext();
        ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        Queue queue = (Queue) remoteContext.lookup("queue/corrupted");
        try (JMSContext context = cf.createContext("guest", "guest", JMSContext.AUTO_ACKNOWLEDGE);) {
            JMSProducer producer = context.createProducer();
            for (int i = 0; i < 100; i++) {
                producer.send(queue, context.createTextMessage(RandomStringUtils.randomAlphabetic(20000)));
            }
        }
        setJournalSize(200L);
        remoteContext.close();
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
        remoteContext = createJNDIContext();
        cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        queue = (Queue) remoteContext.lookup("queue/corrupted");
        try (JMSContext context = cf.createContext("guest", "guest", JMSContext.AUTO_ACKNOWLEDGE);) {
            JMSConsumer consumer = context.createConsumer(queue);
            for (int i = 0; i < 100; i++) {
                TextMessage message = (TextMessage) consumer.receive(TimeoutUtil.adjust(500));
                Assert.assertNotNull(message);
            }
        }
        Path attic = getAtticPath();
        Assert.assertTrue("Couldn't find " + attic.toAbsolutePath(), Files.exists(attic));
        Assert.assertTrue(Files.isDirectory(attic));
        try (Stream<Path> stream = Files.list(attic)){
            long nbAtticFiles = stream.collect(Collectors.counting());
            Assert.assertTrue(nbAtticFiles > 0L);
        }
    }

    /**
     * Set the journal max attic files to 0 (aka don't store corrupted journal files).
     * Set the journal file size to 100k.
     * Send 100 messages of 20k.
     * Set the journal file size to 200k.
     * Restart the server.
     * Consume the messages.
     * Shouldn't have journal files in the attic.
     *
     * @throws javax.naming.NamingException
     * @throws java.io.IOException
     */
    @Test
    public void testCorruptedJournalNotSaved() throws NamingException, IOException {
        Path attic = getAtticPath();
        counter++;
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        managementClient.getControllerClient().execute(Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "journal-max-attic-files", 0));
        jmsOperations.close();
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        PathUtil.deleteRecursively(attic);
        container.start(DEFAULT_FULL_JBOSSAS);
        Assert.assertFalse("Couldn't find " + attic.toAbsolutePath(), Files.exists(attic));
        managementClient = createManagementClient();
        InitialContext remoteContext = createJNDIContext();
        ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        Queue queue = (Queue) remoteContext.lookup("queue/corrupted");
        try (JMSContext context = cf.createContext("guest", "guest", JMSContext.AUTO_ACKNOWLEDGE);) {
            JMSProducer producer = context.createProducer();
            for (int i = 0; i < 100; i++) {
                producer.send(queue, context.createTextMessage(RandomStringUtils.randomAlphabetic(20000)));
            }
        }
        setJournalSize(200L);
        remoteContext.close();
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
        remoteContext = createJNDIContext();
        cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteConnectionFactory");
        queue = (Queue) remoteContext.lookup("queue/corrupted");
        try (JMSContext context = cf.createContext("guest", "guest", JMSContext.AUTO_ACKNOWLEDGE);) {
            JMSConsumer consumer = context.createConsumer(queue);
            for (int i = 0; i < 100; i++) {
                TextMessage message = (TextMessage) consumer.receive(TimeoutUtil.adjust(500));
                Assert.assertNotNull(message);
            }
        }
        if (Files.exists(attic)) {
            Assert.assertTrue(Files.isDirectory(attic));
            try ( Stream<Path> stream = Files.list(attic)) {
                long nbAtticFiles = stream.collect(Collectors.counting());
                Assert.assertEquals("We shouldn't have any file in the attic", 0L, nbAtticFiles);
            }
        }
    }

    private void setJournalSize(long size) throws IOException {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        managementClient.getControllerClient().execute(Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "journal-file-size", 1024L * size));
        jmsOperations.close();
    }

    private static ManagementClient createManagementClient() throws UnknownHostException {
        return new ManagementClient(
                TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort(),
                "remote+http");
    }

    protected static InitialContext createJNDIContext() throws NamingException {
        final Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
        String ipAdddress = TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress());
        env.put(Context.PROVIDER_URL, System.getProperty(Context.PROVIDER_URL, "remote+http://" + ipAdddress + ":8080"));
        env.put(Context.SECURITY_PRINCIPAL, "guest");
        env.put(Context.SECURITY_CREDENTIALS, "guest");
        return new InitialContext(env);
    }

    private Path getAtticPath() {
        Path jbossHome = new File(System.getProperty("jboss.home", "jboss-as")).toPath();
        return jbossHome.resolve("standalone").resolve("data").resolve("activemq").resolve("journal").resolve("attic");
    }

}
