/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import org.apache.activemq.artemis.api.core.BroadcastEndpoint;
import org.apache.activemq.artemis.api.core.BroadcastEndpointFactory;

/**
 * A {@link BroadcastEndpointFactory} based on a {@link org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory}.
 * @author Paul Ferraro
 */
@SuppressWarnings("serial")
public class CommandDispatcherBroadcastEndpointFactory implements BroadcastEndpointFactory {

    private final BroadcastCommandDispatcherFactory factory;
    private final String name;

    public CommandDispatcherBroadcastEndpointFactory(BroadcastCommandDispatcherFactory factory, String name) {
        this.factory = factory;
        this.name = name;
    }

    @Override
    public BroadcastEndpoint createBroadcastEndpoint() throws Exception {
        return new CommandDispatcherBroadcastEndpoint(this.factory, this.name, this.factory, QueueBroadcastManager::new);
    }
}
