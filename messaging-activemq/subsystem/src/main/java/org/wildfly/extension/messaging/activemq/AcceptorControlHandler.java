/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import org.apache.activemq.artemis.api.core.management.AcceptorControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.jboss.as.controller.PathAddress;

/**
 * Handler for runtime operations that interact with a ActiveMQ {@link AcceptorControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class AcceptorControlHandler extends AbstractActiveMQComponentControlHandler<AcceptorControl> {

    public static final AcceptorControlHandler INSTANCE = new AcceptorControlHandler();

    @Override
    protected AcceptorControl getActiveMQComponentControl(ActiveMQBroker activeMQBroker, PathAddress address) {
        final String resourceName = address.getLastElement().getValue();
        return AcceptorControl.class.cast(activeMQBroker.getResource(ResourceNames.ACCEPTOR + resourceName));
    }

    @Override
    protected String getDescriptionPrefix() {
        return CommonAttributes.ACCEPTOR;
    }
}
