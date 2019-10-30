/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.mdb.timeout;

import java.io.IOException;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.dmr.ModelNode;

public class TransactionTimeoutQueueSetupTask implements ServerSetupTask {

    public static final String NO_TIMEOUT_QUEUE_NAME = "noTimeoutQueue";
    public static final String NO_TIMEOUT_JNDI_NAME = "queue/" + NO_TIMEOUT_QUEUE_NAME;
    public static final String DEFAULT_TIMEOUT_QUEUE_NAME = "defaultTimeoutQueue";
    public static final String DEFAULT_TIMEOUT_JNDI_NAME = "queue/" + DEFAULT_TIMEOUT_QUEUE_NAME;
    public static final String ANNOTATION_TIMEOUT_QUEUE_NAME = "annotationTimeoutQueue";
    public static final String ANNOTATION_TIMEOUT_JNDI_NAME = "queue/" + ANNOTATION_TIMEOUT_QUEUE_NAME;
    public static final String PROPERTY_TIMEOUT_QUEUE_NAME = "propertyTimeoutQueue";
    public static final String PROPERTY_TIMEOUT_JNDI_NAME = "queue/" + PROPERTY_TIMEOUT_QUEUE_NAME;

    public static final String REPLY_QUEUE_NAME = "replyQueue";
    public static final String REPLY_QUEUE_JNDI_NAME = "queue/" + REPLY_QUEUE_NAME;

    private ModelNode amqServerDefaultAdress = new ModelNode().add(ClientConstants.SUBSYSTEM, "messaging-activemq")
            .add("server", "default");

    private JMSOperations adminOperations;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        adminOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        adminOperations.createJmsQueue(NO_TIMEOUT_QUEUE_NAME, NO_TIMEOUT_JNDI_NAME);
        adminOperations.createJmsQueue(DEFAULT_TIMEOUT_QUEUE_NAME, DEFAULT_TIMEOUT_JNDI_NAME);
        adminOperations.createJmsQueue(ANNOTATION_TIMEOUT_QUEUE_NAME, ANNOTATION_TIMEOUT_JNDI_NAME);
        adminOperations.createJmsQueue(PROPERTY_TIMEOUT_QUEUE_NAME, PROPERTY_TIMEOUT_JNDI_NAME);
        adminOperations.createJmsQueue(REPLY_QUEUE_NAME, REPLY_QUEUE_JNDI_NAME);

        setMaxDeliveryAttempts(PROPERTY_TIMEOUT_QUEUE_NAME);
        setMaxDeliveryAttempts(DEFAULT_TIMEOUT_QUEUE_NAME);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        if (adminOperations != null) {
            try {
                adminOperations.removeJmsQueue(NO_TIMEOUT_QUEUE_NAME);
                adminOperations.removeJmsQueue(DEFAULT_TIMEOUT_QUEUE_NAME);
                adminOperations.removeJmsQueue(ANNOTATION_TIMEOUT_QUEUE_NAME);
                adminOperations.removeJmsQueue(PROPERTY_TIMEOUT_QUEUE_NAME);
                adminOperations.removeJmsQueue(REPLY_QUEUE_NAME);

                removeAddressSettings(DEFAULT_TIMEOUT_QUEUE_NAME);
                removeAddressSettings(PROPERTY_TIMEOUT_QUEUE_NAME);
            } finally {
                adminOperations.close();
            }
        }
    }

    // /subsystem=messaging-activemq/server=default/address-setting=jms.queue.propertyTimeoutQueue:add(max-delivery-attempts=1)
    private void setMaxDeliveryAttempts(String queueName) throws IOException {
        ModelNode address = amqServerDefaultAdress.clone().add("address-setting", "jms.queue." + queueName);

        ModelNode operation = new ModelNode();
        operation.get(ClientConstants.OP).set(ClientConstants.ADD);
        operation.get(ClientConstants.OP_ADDR).set(address);
        operation.get("max-delivery-attempts").set(1);
        adminOperations.getControllerClient().execute(operation);
    }

    private void removeAddressSettings(String queueName) throws IOException {
        ModelNode address = amqServerDefaultAdress.clone().add("address-setting", "jms.queue." + queueName);

        ModelNode operation = new ModelNode();
        operation.get(ClientConstants.OP).set(ClientConstants.REMOVE_OPERATION);
        operation.get(ClientConstants.OP_ADDR).set(address);
        adminOperations.getControllerClient().execute(operation);
    }
}
