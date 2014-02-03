/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.common.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import java.io.IOException;

/**
 * A default implementation of JMSOperations used with hornetq
 * @author jpai, refactored by <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
public class DefaultHornetQProviderJMSOperations implements JMSOperations {

    private final ManagementClient client;

    private static final Logger logger = Logger.getLogger(DefaultHornetQProviderJMSOperations.class);

    public DefaultHornetQProviderJMSOperations(ManagementClient client) {
        this.client = client;
    }

    @Override
    public void createJmsQueue(String queueName, String jndiName) {
        createJmsDestination("jms-queue", queueName, jndiName);
    }

    @Override
    public void createJmsTopic(String topicName, String jndiName) {
        createJmsDestination("jms-topic", topicName, jndiName);
    }

    @Override
    public void removeJmsQueue(String queueName) {
        removeJmsDestination("jms-queue", queueName);
    }

    @Override
    public void removeJmsTopic(String topicName) {
        removeJmsDestination("jms-topic", topicName);
    }

    @Override
     public void close() {
        // no-op
        // DO NOT close the management client. Whoever passed it into the constructor should close it
    }

    private ModelControllerClient getModelControllerClient() {
        return client.getControllerClient();
    }

    private void createJmsDestination(final String destinationType, final String destinationName, final String jndiName) {
        final ModelNode createJmsQueueOperation = new ModelNode();
        createJmsQueueOperation.get(ClientConstants.OP).set(ClientConstants.ADD);
        createJmsQueueOperation.get(ClientConstants.OP_ADDR).add("subsystem", "messaging");
        createJmsQueueOperation.get(ClientConstants.OP_ADDR).add("hornetq-server", "default");
        createJmsQueueOperation.get(ClientConstants.OP_ADDR).add(destinationType, destinationName);
        createJmsQueueOperation.get("entries").add(jndiName);
        try {
            this.applyUpdate(createJmsQueueOperation);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void removeJmsDestination(final String destinationType, final String destinationName) {
        final ModelNode removeJmsQueue = new ModelNode();
        removeJmsQueue.get(ClientConstants.OP).set("remove");
        removeJmsQueue.get(ClientConstants.OP_ADDR).add("subsystem", "messaging");
        removeJmsQueue.get(ClientConstants.OP_ADDR).add("hornetq-server", "default");
        removeJmsQueue.get(ClientConstants.OP_ADDR).add(destinationType, destinationName);
        try {
            this.applyUpdate(removeJmsQueue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void applyUpdate(final ModelNode update) throws IOException, JMSOperationsException {
        ModelNode result = this.getModelControllerClient().execute(update);
        if (result.hasDefined(ClientConstants.OUTCOME) && ClientConstants.SUCCESS.equals(result.get(ClientConstants.OUTCOME).asString())) {
            logger.info("Operation successful for update = " + update.toString());
        } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
            throw new JMSOperationsException(failureDesc);
        } else {
            throw new JMSOperationsException("Operation not successful; outcome = " + result.get(ClientConstants.OUTCOME));
        }
    }

    @Override
    public void setSystemProperties(String destination, String resourceAdapter) {
        final ModelNode enableSubstitutionOp = new ModelNode();
        enableSubstitutionOp.get(OP_ADDR).set(SUBSYSTEM, "ee");
        enableSubstitutionOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        enableSubstitutionOp.get(NAME).set("annotation-property-replacement");
        enableSubstitutionOp.get(VALUE).set(true);

        final ModelNode setDestinationOp = new ModelNode();
        setDestinationOp.get(ClientConstants.OP).set(ClientConstants.ADD);
        setDestinationOp.get(ClientConstants.OP_ADDR).add("system-property", "destination");
        setDestinationOp.get("value").set(destination);
        final ModelNode setResourceAdapterOp = new ModelNode();
        setResourceAdapterOp.get(ClientConstants.OP).set(ClientConstants.ADD);
        setResourceAdapterOp.get(ClientConstants.OP_ADDR).add("system-property", "resource.adapter");
        setResourceAdapterOp.get("value").set(resourceAdapter);

        try {
            applyUpdate(enableSubstitutionOp);
            applyUpdate(setDestinationOp);
            applyUpdate(setResourceAdapterOp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeSystemProperties() {
        final ModelNode removeDestinationOp = new ModelNode();
        removeDestinationOp.get(ClientConstants.OP).set("remove");
        removeDestinationOp.get(ClientConstants.OP_ADDR).add("system-property", "destination");
        final ModelNode removeResourceAdapterOp = new ModelNode();
        removeResourceAdapterOp.get(ClientConstants.OP).set("remove");
        removeResourceAdapterOp.get(ClientConstants.OP_ADDR).add("system-property", "resource.adapter");

        final ModelNode disableSubstitutionOp = new ModelNode();
        disableSubstitutionOp.get(OP_ADDR).set(SUBSYSTEM, "ee");
        disableSubstitutionOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        disableSubstitutionOp.get(NAME).set("annotation-property-replacement");
        disableSubstitutionOp.get(VALUE).set(false);

        try {
            applyUpdate(removeDestinationOp);
            applyUpdate(removeResourceAdapterOp);
            applyUpdate(disableSubstitutionOp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
