/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.jms.external;


import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that invoking a management operation that removes a JMS resource that is used by a deployed archive must fail:
 * the resource must not be removed and any depending services must be recovered.
 * The deployment must still be operating after the failing management operation.

 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ExternalMessagingDeploymentRemoteTestCase.SetupTask.class)
public class ExternalMessagingDeploymentRemoteTestCase {

    public static final String QUEUE_LOOKUP = "java:/jms/DependentMessagingDeploymentTestCase/myQueue";
    public static final String TOPIC_LOOKUP = "java:/jms/DependentMessagingDeploymentTestCase/myTopic";
    public static final String REMOTE_PCF = "remote-artemis";

    private static final String QUEUE_NAME = "myQueue";
    private static final String TOPIC_NAME = "myTopic";

    @ArquillianResource
    private URL url;

    static class SetupTask extends SnapshotRestoreSetupTask {

        private static final Logger logger = Logger.getLogger(ExternalMessagingDeploymentRemoteTestCase.SetupTask.class);

        @Override
        public void doSetup(org.jboss.as.arquillian.container.ManagementClient managementClient, String s) throws Exception {
            JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            ops.createJmsQueue(QUEUE_NAME, "/queue/" + QUEUE_NAME);
            ops.createJmsTopic(TOPIC_NAME, "/topic/" + TOPIC_NAME);
            execute(managementClient, addSocketBinding("legacy-messaging", 5445) , true);
            execute(managementClient, addExternalRemoteConnector(ops.getSubsystemAddress(), "remote-broker-connector", "legacy-messaging") , true);
            execute(managementClient, addRemoteAcceptor(ops.getServerAddress(), "legacy-messaging-acceptor", "legacy-messaging") , true);
            ModelNode op = Operations.createRemoveOperation(getInitialPooledConnectionFactoryAddress(ops.getServerAddress()));
            execute(managementClient, op, true);
            op = Operations.createAddOperation(getPooledConnectionFactoryAddress());
            op.get("transaction").set("xa");
            op.get("entries").add("java:/JmsXA java:jboss/DefaultJMSConnectionFactory");
            op.get("connectors").add("remote-broker-connector");
            execute(managementClient, op, true);
            op = Operations.createAddOperation(getExternalTopicAddress());
            op.get("entries").add(TOPIC_LOOKUP);
            op.get("entries").add("/topic/myAwesomeClientTopic");
            execute(managementClient, op, true);
            op = Operations.createAddOperation(getExternalQueueAddress());
            op.get("entries").add(QUEUE_LOOKUP);
            op.get("entries").add("/queue/myAwesomeClientQueue");
            execute(managementClient, op, true);
            ServerReload.executeReloadAndWaitForCompletion(managementClient.getControllerClient());
        }

        private ModelNode execute(final org.jboss.as.arquillian.container.ManagementClient managementClient, final ModelNode op, final boolean expectSuccess) throws IOException {
            ModelNode response = managementClient.getControllerClient().execute(op);
            final String outcome = response.get("outcome").asString();
            if (expectSuccess) {
                assertEquals(response.toString(), "success", outcome);
                return response.get("result");
            } else {
                assertEquals("failed", outcome);
                return response.get("failure-description");
            }
        }

        ModelNode getPooledConnectionFactoryAddress() {
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("pooled-connection-factory", REMOTE_PCF);
            return address;
        }


        ModelNode getExternalTopicAddress() {
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("external-jms-topic", TOPIC_NAME);
            return address;
        }


        ModelNode getExternalQueueAddress() {
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("external-jms-queue", QUEUE_NAME);
            return address;
        }

        ModelNode getInitialPooledConnectionFactoryAddress(ModelNode serverAddress) {
            ModelNode address = serverAddress.clone();
            address.add("pooled-connection-factory", "activemq-ra");
            return address;
        }

        ModelNode addSocketBinding(String bindingName, int port) {
            ModelNode address = new ModelNode();
            address.add("socket-binding-group", "standard-sockets");
            address.add("socket-binding", bindingName);

            ModelNode socketBindingOp = new ModelNode();
            socketBindingOp.get(OP).set(ADD);
            socketBindingOp.get(OP_ADDR).set(address);
            socketBindingOp.get("port").set(port);
            return socketBindingOp;
        }

        ModelNode addExternalRemoteConnector(ModelNode subsystemAddress, String name, String socketBinding) {
            ModelNode address = subsystemAddress.clone();
            address.add("remote-connector", name);

            ModelNode socketBindingOp = new ModelNode();
            socketBindingOp.get(OP).set(ADD);
            socketBindingOp.get(OP_ADDR).set(address);
            socketBindingOp.get("socket-binding").set(socketBinding);
            return socketBindingOp;
        }

        ModelNode addRemoteAcceptor(ModelNode serverAddress, String name, String socketBinding) {
            ModelNode address = serverAddress.clone();
            address.add("remote-acceptor", name);

            ModelNode socketBindingOp = new ModelNode();
            socketBindingOp.get(OP).set(ADD);
            socketBindingOp.get(OP_ADDR).set(address);
            socketBindingOp.get("socket-binding").set(socketBinding);
            return socketBindingOp;
        }
    }

    @Deployment
    public static WebArchive createArchive() {
        return create(WebArchive.class, "ClientMessagingDeploymentTestCase.war")
                .addClass(MessagingServlet.class)
                .addClasses(QueueMDB.class, TopicMDB.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testSendMessageInClientQueue() throws Exception {
        sendAndReceiveMessage(true);
    }

    @Test
    public void testSendMessageInClientTopic() throws Exception {
        sendAndReceiveMessage(false);
    }

    private void sendAndReceiveMessage(boolean sendToQueue) throws Exception {
        String destination = sendToQueue ? "queue" : "topic";
        String text = UUID.randomUUID().toString();
        URL url = new URL(this.url.toExternalForm() + "ClientMessagingDeploymentTestCase?destination=" + destination + "&text=" + text);
        String reply = HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);

        assertNotNull(reply);
        assertEquals(text, reply);
    }
}
