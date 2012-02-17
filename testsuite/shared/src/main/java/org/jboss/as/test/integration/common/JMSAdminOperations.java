/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.common;

import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Jaikiran Pai
 */
public class JMSAdminOperations {


    private final ModelControllerClient modelControllerClient;

    private static final Logger logger = Logger.getLogger(JMSAdminOperations.class);

    public JMSAdminOperations() {
        this("localhost", 9999);

    }

    public JMSAdminOperations(final String hostName, final int port) {
        try {
            this.modelControllerClient = ModelControllerClient.Factory.create(InetAddress.getByName(hostName), port, getCallbackHandler());
        } catch (UnknownHostException e) {
            throw new RuntimeException("Cannot create model controller client for host: " + hostName + " and port " + port, e);
        }
    }

    public void close() {
        try {
            modelControllerClient.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ModelControllerClient getModelControllerClient() {
        return modelControllerClient;
    }

    public void createJmsQueue(final String queueName, final String jndiName) {
        createJmsDestination("jms-queue", queueName, jndiName);
    }

    public void createJmsTopic(final String topicName, final String jndiName) {
        createJmsDestination("jms-topic", topicName, jndiName);
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

    public void removeJmsQueue(final String queueName) {
        removeJmsDestination("jms-queue", queueName);
    }

    public void removeJmsTopic(final String topicName) {
        removeJmsDestination("jms-topic", topicName);
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

    private void applyUpdate(final ModelNode update) throws IOException, JMSAdminOperationException {
        ModelNode result = this.modelControllerClient.execute(update);
        if (result.hasDefined(ClientConstants.OUTCOME) && ClientConstants.SUCCESS.equals(result.get(ClientConstants.OUTCOME).asString())) {
            logger.info("Operation successful for update = " + update.toString());
            return;
        } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
            throw new JMSAdminOperationException(failureDesc);
        } else {
            throw new JMSAdminOperationException("Operation not successful; outcome = " + result.get(ClientConstants.OUTCOME));
        }

    }

    /**
     * JMSAdminOperationException
     */
    private class JMSAdminOperationException extends Exception {

        JMSAdminOperationException(final String msg, final Throwable cause) {
            super(msg, cause);
        }

        JMSAdminOperationException(final String msg) {
            super(msg);
        }
    }
}
