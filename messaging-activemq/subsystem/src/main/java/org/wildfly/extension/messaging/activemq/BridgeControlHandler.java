/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.apache.activemq.artemis.api.core.management.BridgeControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.jboss.as.controller.PathAddress;

/**
 * Handler for runtime operations that interact with a ActiveMQ {@link BridgeControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BridgeControlHandler extends AbstractActiveMQComponentControlHandler<BridgeControl> {

    public static final BridgeControlHandler INSTANCE = new BridgeControlHandler();

    @Override
    protected BridgeControl getActiveMQComponentControl(ActiveMQBroker activeMQBroker, PathAddress address) {
        final String resourceName = address.getLastElement().getValue();
        return BridgeControl.class.cast(activeMQBroker.getResource(ResourceNames.BRIDGE + resourceName));
    }

    @Override
    protected String getDescriptionPrefix() {
        return CommonAttributes.BRIDGE;
    }
}
