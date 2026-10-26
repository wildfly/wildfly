/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.mgmt;

import java.io.IOException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
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
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class CriticalAnalyzerTestCase extends ContainerResourceMgmtTestBase {

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testCriticalAnalyzerWrite() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        ModelNode op = Operations.createReadResourceOperation(jmsOperations.getServerAddress());
        op.get("include-defaults").set(true);
        op.get("attributes-only").set(true);
        ModelNode server = executeOperation(op, true);
        Assertions.assertTrue(server.hasDefined("critical-analyzer-enabled"));
        Assertions.assertTrue(server.get("critical-analyzer-enabled").asBoolean());
        Assertions.assertTrue(server.hasDefined("critical-analyzer-policy"));
        Assertions.assertEquals("LOG", server.get("critical-analyzer-policy").asString());
        Assertions.assertTrue(server.hasDefined("critical-analyzer-timeout"));
        Assertions.assertEquals(120000L, server.get("critical-analyzer-timeout").asLong());
        Assertions.assertTrue(server.hasDefined("critical-analyzer-check-period"));
        Assertions.assertEquals(0L, server.get("critical-analyzer-check-period").asLong());
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-enabled", ModelNode.FALSE);
        executeOperationForSuccess(op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-policy", new ModelNode("HALT"));
        executeOperationForSuccess(op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-timeout", new ModelNode(240000L));
        executeOperationForSuccess(op);
        op = Operations.createWriteAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-check-period", new ModelNode(120000L));
        executeOperationForSuccess(op);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
        jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        op = Operations.createReadResourceOperation(jmsOperations.getServerAddress());
        op.get("include-defaults").set(true);
        op.get("attributes-only").set(true);
        server = executeOperation(op, true);
        Assertions.assertTrue(server.hasDefined("critical-analyzer-enabled"));
        Assertions.assertFalse(server.get("critical-analyzer-enabled").asBoolean());
        Assertions.assertTrue(server.hasDefined("critical-analyzer-policy"));
        Assertions.assertEquals("HALT", server.get("critical-analyzer-policy").asString());
        Assertions.assertTrue(server.hasDefined("critical-analyzer-timeout"));
        Assertions.assertEquals(240000L, server.get("critical-analyzer-timeout").asLong());
        Assertions.assertTrue(server.hasDefined("critical-analyzer-check-period"));
        Assertions.assertEquals(120000L, server.get("critical-analyzer-check-period").asLong());
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-enabled");
        executeOperationForSuccess(op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-policy");
        executeOperationForSuccess(op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-timeout");
        executeOperationForSuccess(op);
        op = Operations.createUndefineAttributeOperation(jmsOperations.getServerAddress(), "critical-analyzer-check-period");
        executeOperationForSuccess(op);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    private void executeOperationForSuccess(ModelNode operation) throws IOException, MgmtOperationException {
        ModelNode result = executeOperation(operation, false);
        Assertions.assertTrue(isSuccessfulOutcome(result), Util.getFailureDescription(result));
    }
}
