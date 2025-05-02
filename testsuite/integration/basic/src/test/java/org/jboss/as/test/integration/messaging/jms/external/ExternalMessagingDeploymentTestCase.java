/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.external;


import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.SocketPermission;
import java.net.URL;
import java.util.PropertyPermission;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.ExtendedSnapshotServerSetupTask;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that invoking a management operation that removes a Jakarta Messaging resource that is used by a deployed archive must fail:
 * the resource must not be removed and any depending services must be recovered.
 * The deployment must still be operating after the failing management operation.

 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ExternalMessagingDeploymentTestCase.SetupTask.class)
public class ExternalMessagingDeploymentTestCase {

    public static final String QUEUE_LOOKUP = "java:/jms/DependentMessagingDeploymentTestCase/myQueue";
    public static final String TOPIC_LOOKUP = "java:/jms/DependentMessagingDeploymentTestCase/myTopic";
    public static final String REMOTE_PCF = "remote-artemis";

    private static final String QUEUE_NAME = "myQueue";
    private static final String TOPIC_NAME = "myTopic";

    @ArquillianResource
    private URL url;

    static class SetupTask extends ExtendedSnapshotServerSetupTask {

        private static final Logger logger = Logger.getLogger(ExternalMessagingDeploymentTestCase.SetupTask.class);

        @Override
        public void doSetup(org.jboss.as.arquillian.container.ManagementClient managementClient, String s) throws Exception {
            ServerReload.executeReloadAndWaitForCompletion(managementClient, true);
            JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            ops.createJmsQueue(QUEUE_NAME, "/queue/" + QUEUE_NAME);
            ops.createJmsTopic(TOPIC_NAME, "/topic/" + TOPIC_NAME);
            boolean needRemoteConnector = ops.isRemoteBroker();
            if(needRemoteConnector) {
                ops.addExternalRemoteConnector("remote-broker-connector", "messaging-activemq");
            } else {
                ops.addExternalHttpConnector("http-test-connector", "http", "http-acceptor");
            }
            ModelNode op = Operations.createRemoveOperation(getInitialPooledConnectionFactoryAddress(ops.getServerAddress()));
            managementClient.getControllerClient().execute(op);
            op = Operations.createAddOperation(getPooledConnectionFactoryAddress());
            op.get("transaction").set("xa");
            op.get("entries").add("java:/JmsXA java:jboss/DefaultJMSConnectionFactory");
            if(needRemoteConnector) {
                op.get("connectors").add("remote-broker-connector");
            } else {
                op.get("connectors").add("http-test-connector");
            }
            execute(managementClient, op, true);
            op = Operations.createRemoveOperation(getClientTopicAddress());
            managementClient.getControllerClient().execute(op);
            op = Operations.createAddOperation(getClientTopicAddress());
            op.get("entries").add(TOPIC_LOOKUP);
            op.get("entries").add("/topic/myAwesomeClientTopic");
            execute(managementClient, op, true);
            op = Operations.createRemoveOperation(getClientQueueAddress());
            managementClient.getControllerClient().execute(op);
            op = Operations.createAddOperation(getClientQueueAddress());
            op.get("entries").add(QUEUE_LOOKUP);
            op.get("entries").add("/queue/myAwesomeClientQueue");
            execute(managementClient, op, true);
            ops.close();
            ServerReload.executeReloadAndWaitForCompletion(managementClient);
        }

        @Override
        protected void beforeRestore(final ManagementClient managementClient, final String containerId) throws Exception {
            final JMSOperations ops = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
            ops.removeJmsQueue(QUEUE_NAME);
            ops.removeJmsTopic(TOPIC_NAME);
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


        ModelNode getClientTopicAddress() {
            ModelNode address = new ModelNode();
            address.add("subsystem", "messaging-activemq");
            address.add("external-jms-topic", TOPIC_NAME);
            return address;
        }


        ModelNode getClientQueueAddress() {
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
    }

    @Deployment
    public static WebArchive createArchive() {
        return create(WebArchive.class, "ClientMessagingDeploymentTestCase.war")
                .addClasses(MessagingServlet.class, TimeoutUtil.class)
                .addClasses(QueueMDB.class, TopicMDB.class)
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                        new SocketPermission("localhost", "resolve"),
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")), "permissions.xml")
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
        String serverUrl = this.url.toExternalForm();
        if (!serverUrl.endsWith("/")) {
            serverUrl = serverUrl + "/";
        }
        URL servletUrl = new URL(serverUrl + "ClientMessagingDeploymentTestCase?destination=" + destination + "&text=" + text);
        String reply = HttpRequest.get(servletUrl.toExternalForm(), TimeoutUtil.adjust(10), TimeUnit.SECONDS);

        assertNotNull(reply);
        assertEquals(text, reply);
    }
}
