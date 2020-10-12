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
package org.jboss.as.test.integration.messaging.mgmt;

import java.io.IOException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.cli.Util;
import org.jboss.as.controller.client.helpers.Operations;

import static org.jboss.as.controller.client.helpers.Operations.isSuccessfulOutcome;

import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class NetworkHealthTestCase extends ContainerResourceMgmtTestBase {

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testNetworkHealthWrite() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        // <jms server address>/address-setting=jms.queue.foo:write-attribute(name=redelivery-delay,value=50)
        ModelNode op = Operations.createReadResourceOperation(jmsOperations.getServerAddress());
        op.get("include-defaults").set(true);
        op.get("attributes-only").set(true);
        ModelNode server = executeOperation(op, true);
        Assert.assertFalse(server.hasDefined("network-check-list"));
        Assert.assertFalse(server.hasDefined("network-check-nic"));
        Assert.assertFalse(server.hasDefined("network-check-url-list"));
        Assert.assertEquals(5000L, server.get("network-check-period").asLong());
        Assert.assertEquals(1000L, server.get("network-check-timeout").asLong());
        Assert.assertEquals("ping -c 1 -t %d %s", server.get("network-check-ping-command").asString());
        Assert.assertEquals("ping6 -c 1 %2$s", server.get("network-check-ping6-command").asString());
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-list", TestSuiteEnvironment.getServerAddress());
        executeOperationForSuccess(op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-nic", "lo");
        executeOperationForSuccess(op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-url-list", TestSuiteEnvironment.getHttpAddress());
        executeOperationForSuccess(op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-period", 10000L);
        executeOperationForSuccess(op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-timeout", 5000L);
        executeOperationForSuccess(op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-ping-command", "ping");
        executeOperationForSuccess(op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "network-check-ping6-command", "ping6");
        executeOperationForSuccess(op);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
        jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        op = Operations.createReadResourceOperation(jmsOperations.getServerAddress());
        op.get("include-defaults").set(true);
        op.get("attributes-only").set(true);
        server = executeOperation(op, true);
        Assert.assertEquals(server.toJSONString(false), TestSuiteEnvironment.getHttpAddress(), server.get("network-check-list").asString());
        Assert.assertEquals(server.toJSONString(false), "lo", server.get("network-check-nic").asString());
        Assert.assertEquals(server.toJSONString(false), TestSuiteEnvironment.getHttpAddress(), server.get("network-check-url-list").asString());
        Assert.assertEquals(server.toJSONString(false), 10000L, server.get("network-check-period").asLong());
        Assert.assertEquals(server.toJSONString(false), 5000L, server.get("network-check-timeout").asLong());
        Assert.assertEquals(server.toJSONString(false), "ping", server.get("network-check-ping-command").asString());
        Assert.assertEquals(server.toJSONString(false), "ping6", server.get("network-check-ping6-command").asString());
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-list");
        executeOperationForSuccess(op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-nic");
        executeOperationForSuccess(op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-url-list");
        executeOperationForSuccess(op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-period");
        executeOperationForSuccess(op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-timeout");
        executeOperationForSuccess(op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-ping-command");
        executeOperationForSuccess(op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "network-check-ping6-command");
        executeOperationForSuccess(op);
    }

    private void executeOperationForSuccess(ModelNode operation) throws IOException, MgmtOperationException {
        ModelNode result = executeOperation(operation, false);
        Assert.assertTrue(Util.getFailureDescription(result), isSuccessfulOutcome(result));
    }
}
