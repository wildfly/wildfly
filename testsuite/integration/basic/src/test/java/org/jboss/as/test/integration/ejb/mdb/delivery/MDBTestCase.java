/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.delivery;

import static org.jboss.as.controller.client.helpers.ClientConstants.NAME;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP;
import static org.jboss.as.controller.client.helpers.ClientConstants.OP_ADDR;
import static org.jboss.as.controller.client.helpers.ClientConstants.OUTCOME;
import static org.jboss.as.controller.client.helpers.ClientConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;
import static org.jboss.as.controller.client.helpers.ClientConstants.SUCCESS;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FilePermission;
import java.io.IOException;
import java.util.PropertyPermission;

import jakarta.annotation.Resource;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.remoting3.security.RemotingPermission;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests MDB DeliveryActive property + management methods to start/stop delivering messages.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
@ServerSetup({MDBTestCase.JmsQueueSetup.class})
public class MDBTestCase {

    private static final Logger logger = Logger.getLogger(MDBTestCase.class);

    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory cf;

    @Resource(mappedName = "java:jboss/deliveryactive/MDBWithAnnotationQueue")
    private Queue annotationQueue;

    @Resource(mappedName = "java:jboss/deliveryactive/MDBWithDeploymentDescriptorQueue")
    private Queue deploymentDescriptorQueue;

    @ArquillianResource
    private ManagementClient managementClient;

    private static final int TIMEOUT = TimeoutUtil.adjust(5000);

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue("deliveryactive/MDBWithAnnotationQueue", "java:jboss/deliveryactive/MDBWithAnnotationQueue");
            jmsAdminOperations.createJmsQueue("deliveryactive/MDBWithDeploymentDescriptorQueue", "java:jboss/deliveryactive/MDBWithDeploymentDescriptorQueue");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("deliveryactive/MDBWithAnnotationQueue");
                jmsAdminOperations.removeJmsQueue("deliveryactive/MDBWithDeploymentDescriptorQueue");
                jmsAdminOperations.close();
            }
        }
    }

    @Deployment
    public static Archive getDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "mdb.jar")
                .addPackage(MDBWithDeliveryActiveAnnotation.class.getPackage())
                .addPackage(JMSOperations.class.getPackage())
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(MDBWithDeliveryActiveAnnotation.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, org.jboss.remoting\n"), "MANIFEST.MF");
        // grant necessary permissions
        ejbJar.addAsManifestResource(createPermissionsXmlAsset(
                new PropertyPermission("ts.timeout.factor", "read"),
                new RemotingPermission("createEndpoint"),
                new RemotingPermission("connect"),
                new FilePermission(System.getProperty("jboss.inst") + "/standalone/tmp/auth/*", "read")
        ), "permissions.xml");
        return ejbJar;
    }

    @Test
    public void testDeliveryActiveWithAnnotation() throws Exception {
        doDeliveryActive(annotationQueue, "MDBWithDeliveryActiveAnnotation");
    }

    @Test
    public void testDeliveryActiveWithDeploymentDescriptor() throws Exception {
        doDeliveryActive(deploymentDescriptorQueue, "MDBWithDeliveryActiveDeploymentDescriptor");
    }

    private void doDeliveryActive(Destination destination, String mdbName) throws Exception {
        // ReplyingMDB has been deployed with deliveryActive set to false
        assertMDBDeliveryIsActive(mdbName, false);

        Connection connection = null;

        try {
            connection = cf.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            TemporaryQueue replyQueue = session.createTemporaryQueue();

            // send a message to the MDB
            MessageProducer producer = session.createProducer(destination);
            Message message = session.createMessage();
            message.setJMSReplyTo(replyQueue);
            producer.send(message);

            // the MDB did not reply to the message because its delivery is not active
            MessageConsumer consumer = session.createConsumer(replyQueue);
            Message reply = consumer.receive(TIMEOUT);
            assertNull(reply);

            executeMDBOperation(mdbName, "start-delivery");
            assertMDBDeliveryIsActive(mdbName, true);
            // WFLY-4470 check duplicate message when start delivery twice. Last assertNull(reply) should still be valid
            executeMDBOperation(mdbName, "start-delivery");

            // the message was delivered to the MDB which replied
            reply = consumer.receive(TIMEOUT);
            assertNotNull(reply);
            assertEquals(message.getJMSMessageID(), reply.getJMSCorrelationID());

            executeMDBOperation(mdbName, "stop-delivery");
            assertMDBDeliveryIsActive(mdbName, false);

            // send again a message to the MDB
            message = session.createMessage();
            message.setJMSReplyTo(replyQueue);
            producer.send(message);

            // the MDB did not reply to the message because its delivery is not active
            reply = consumer.receive(TIMEOUT);
            assertNull(reply);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private void executeMDBOperation(String mdbName, String opName) throws IOException {
        ModelNode operation = createMDBOperation(mdbName);
        operation.get(OP).set(opName);
        ModelNode result = managementClient.getControllerClient().execute(operation);

        assertTrue(result.toJSONString(true), result.hasDefined(OUTCOME));
        assertEquals(result.toJSONString(true), SUCCESS, result.get(OUTCOME).asString());
    }

    private void assertMDBDeliveryIsActive(String mdbName, boolean expected) throws IOException {
        ModelNode operation = createMDBOperation(mdbName);
        operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
        operation.get(NAME).set("delivery-active");
        ModelNode result = managementClient.getControllerClient().execute(operation);

        assertTrue(result.toJSONString(true), result.hasDefined(OUTCOME));
        assertEquals(result.toJSONString(true), expected, result.get(RESULT).asBoolean());
    }

    private ModelNode createMDBOperation(String mdbName) {
        ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).add("deployment", "mdb.jar");
        operation.get(OP_ADDR).add("subsystem", "ejb3");
        operation.get(OP_ADDR).add("message-driven-bean", mdbName);
        return operation;
    }
}
