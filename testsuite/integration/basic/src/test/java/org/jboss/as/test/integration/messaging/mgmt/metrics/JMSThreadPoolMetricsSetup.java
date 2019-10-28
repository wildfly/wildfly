/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
