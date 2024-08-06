/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.runas.mdb;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;
import jakarta.jms.TextMessage;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.permission.ChangeRoleMapperPermission;
import org.wildfly.security.permission.ElytronPermission;

/**
 * Make sure the run-as on a MDB is picked up.
 * Part of migration test from EAP5 (JBAS-6239) to AS7 [JBQA-5275].
 *
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@ServerSetup({RunAsMDBUnitTestCase.JmsQueueSetup.class})
@Category(CommonCriteria.class)
public class RunAsMDBUnitTestCase {
    private static final Logger log = Logger.getLogger(RunAsMDBUnitTestCase.class.getName());
    private static final String queueName = "queue/mdbtest";

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue(queueName, "java:jboss/" + queueName);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue(queueName);
                jmsAdminOperations.close();
            }
        }
    }

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "runas-mdb.jar")
                .addPackage(RunAsMDBUnitTestCase.class.getPackage())
                .addPackage(JMSOperations.class.getPackage())
                .addClass(JmsQueueSetup.class);
        jar.addAsManifestResource(RunAsMDBUnitTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr \n"), "MANIFEST.MF");
        jar.addPackage(CommonCriteria.class.getPackage());
        // TODO WFLY-15289 Should these permissions be required?
        jar.addAsResource(createPermissionsXmlAsset(new ElytronPermission("setRunAsPrincipal"),
                new ChangeRoleMapperPermission("ejb")), "META-INF/jboss-permissions.xml");
        return jar;
    }

    protected <T> T lookup(String name, Class<T> cls) throws Exception {
        return cls.cast(ctx.lookup(name));
    }

    @Test
    public void testSendMessage() throws Exception {
        ConnectionFactory connFactory = lookup("ConnectionFactory", ConnectionFactory.class);
        Connection conn = connFactory.createConnection();
        conn.start();
        Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        TemporaryQueue replyQueue = session.createTemporaryQueue();
        TextMessage msg = session.createTextMessage("Hello world");
        msg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
        msg.setJMSReplyTo(replyQueue);
        Queue queue = lookup("java:jboss/" + queueName, Queue.class);
        MessageProducer producer = session.createProducer(queue);
        producer.send(msg);
        MessageConsumer consumer = session.createConsumer(replyQueue);
        Message replyMsg = consumer.receive(5000);
        Assert.assertNotNull(replyMsg);
        if (replyMsg instanceof ObjectMessage) {
            Exception e = (Exception) ((ObjectMessage) replyMsg).getObject();
            throw e;
        }
        Assert.assertTrue(replyMsg instanceof TextMessage);
        String actual = ((TextMessage) replyMsg).getText();
        Assert.assertEquals("SUCCESS", actual);

        consumer.close();
        producer.close();
        session.close();
        conn.close();
    }
}
