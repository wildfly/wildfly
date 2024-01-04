/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.mgmt.metrics;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

import java.io.IOException;
import org.jboss.as.controller.client.helpers.Operations;

/**
 * @author Ivan Straka
 */
class JMSThreadPoolMetricsSetup implements ServerSetupTask {

    private JMSOperations jmsAdminOperations;
    private ModelNode rcfConnectors;

    private ModelNode readRemoteCFConnectors(ManagementClient client) throws IOException {
        ModelNode op = new ModelNode();
        op.get("address").set(jmsAdminOperations.getServerAddress().add("connection-factory", "RemoteConnectionFactory"));
        op.get("operation").set("read-attribute");
        op.get("name").set("connectors");

        ModelNode execute = client.getControllerClient().execute(op);
        Assert.assertTrue(Operations.isSuccessfulOutcome(execute));
        return Operations.readResult(execute);
    }

    private void writeRemoteCFConnectors(ManagementClient client, ModelNode connectors) throws IOException, MgmtOperationException {
        ModelNode op = new ModelNode();
        op.get("address").set(jmsAdminOperations.getServerAddress().add("connection-factory", "RemoteConnectionFactory"));
        op.get("operation").set("write-attribute");
        op.get("name").set("connectors");
        op.get("value").set(new ModelNode().add("http-connector-throughput"));
        op.get("value").set(connectors);
        client.getControllerClient().execute(op);

        ServerReload.executeReloadAndWaitForCompletion(client);
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsAdminOperations.createJmsQueue("metrics/queue", "java:jboss/metrics/queue");
        jmsAdminOperations.createJmsQueue("metrics/replyQueue", "java:jboss/metrics/replyQueue");

//      use scheduledThreadPoolExecutor - set http-connector-throughput for RemoteConnectionFactory (it is configured with batch delay)
        rcfConnectors = readRemoteCFConnectors(managementClient);
        writeRemoteCFConnectors(managementClient, new ModelNode().add("http-connector-throughput"));
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (jmsAdminOperations != null) {
            jmsAdminOperations.removeJmsQueue("metrics/queue");
            jmsAdminOperations.removeJmsQueue("metrics/replyQueue");
            writeRemoteCFConnectors(managementClient, rcfConnectors);
            jmsAdminOperations.close();
        }
    }
}
