/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.messaging.activemq;

import org.apache.activemq.artemis.core.config.BridgeConfiguration;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

public class BridgeAddTestCase {

    private static final String BRIDGE_NAME = "CoreBridge";
    private static final String SOURCE_QUEUE_NAME = "SourceQueue";
    private static final String TARGET_QUEUE_NAME = "TargetQueue";

    @Test
    public void testCreateBridgeConfiguration() throws OperationFailedException {
        BridgeConfiguration bridgeConfiguration =
                BridgeAdd.createBridgeConfiguration(ExpressionResolver.SIMPLE, BRIDGE_NAME, createModel());

        Assert.assertEquals(BRIDGE_NAME, bridgeConfiguration.getName());
        Assert.assertEquals(SOURCE_QUEUE_NAME, bridgeConfiguration.getQueueName());
        Assert.assertEquals(TARGET_QUEUE_NAME, bridgeConfiguration.getForwardingAddress());
        Assert.assertArrayEquals(new String[] {"in-vm"}, bridgeConfiguration.getStaticConnectors().toArray());
        Assert.assertEquals(30000, bridgeConfiguration.getCallTimeout());
    }

    @Test
    public void testCreateBridgeConfigurationCallTimeoutInModel() throws OperationFailedException {
        ModelNode model = createModel();
        model.get("call-timeout").set(70000);
        BridgeConfiguration bridgeConfiguration =
                BridgeAdd.createBridgeConfiguration(ExpressionResolver.SIMPLE, BRIDGE_NAME, model);

        Assert.assertEquals(BRIDGE_NAME, bridgeConfiguration.getName());
        Assert.assertEquals(SOURCE_QUEUE_NAME, bridgeConfiguration.getQueueName());
        Assert.assertEquals(TARGET_QUEUE_NAME, bridgeConfiguration.getForwardingAddress());
        Assert.assertArrayEquals(new String[] {"in-vm"}, bridgeConfiguration.getStaticConnectors().toArray());
        Assert.assertEquals(70000, bridgeConfiguration.getCallTimeout());
    }

    @Test
    public void testCreateBridgeConfigurationCallTimeoutSystemProperty() throws OperationFailedException {
        try {
            System.setProperty(BridgeAdd.CALL_TIMEOUT_PROPERTY, "80000");

            BridgeConfiguration bridgeConfiguration =
                    BridgeAdd.createBridgeConfiguration(ExpressionResolver.SIMPLE, BRIDGE_NAME, createModel());

            Assert.assertEquals(BRIDGE_NAME, bridgeConfiguration.getName());
            Assert.assertEquals(SOURCE_QUEUE_NAME, bridgeConfiguration.getQueueName());
            Assert.assertEquals(TARGET_QUEUE_NAME, bridgeConfiguration.getForwardingAddress());
            Assert.assertArrayEquals(new String[] {"in-vm"}, bridgeConfiguration.getStaticConnectors().toArray());
            Assert.assertEquals(80000, bridgeConfiguration.getCallTimeout());
        } finally {
            System.clearProperty(BridgeAdd.CALL_TIMEOUT_PROPERTY);
        }
    }

    private static ModelNode createModel() {
        ModelNode model = new ModelNode();
        model.get("queue-name").set(SOURCE_QUEUE_NAME);
        model.get("forwarding-address").set(TARGET_QUEUE_NAME);
        model.get("static-connectors").add("in-vm");
        return model;
    }
}
