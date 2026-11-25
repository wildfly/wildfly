/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.messaging.client.messaging;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.ServerReload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Demo using the AS management API to create and destroy a Artemis core queue.
 *
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class CoreQueueMessagingTestCase {

    private final String queueName = "queue.standalone";

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testMessagingClientUsingMessagingPort() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.addCoreQueue(queueName, queueName, true, "ANYCAST");
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
        jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.removeCoreQueue(queueName);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
        jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.addCoreQueue(queueName, queueName, true, "MULTICAST");
    }



    @AfterEach
    public void tearDown() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.removeCoreQueue(queueName);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }
}
