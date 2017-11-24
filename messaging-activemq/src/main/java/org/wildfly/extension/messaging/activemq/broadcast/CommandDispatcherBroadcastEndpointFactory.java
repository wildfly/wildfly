/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.broadcast;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.activemq.artemis.api.core.BroadcastEndpoint;
import org.apache.activemq.artemis.api.core.BroadcastEndpointFactory;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;

/**
 * A {@link BroadcastEndpointFactory} based on a {@link CommandDispatcherFactory}.
 * @author Paul Ferraro
 */
@SuppressWarnings("serial")
public class CommandDispatcherBroadcastEndpointFactory implements BroadcastEndpointFactory {

    private static final Map<String, BroadcastManager> BROADCAST_MANAGERS = new ConcurrentHashMap<>();

    private final CommandDispatcherFactory factory;
    private final String name;
    private final BroadcastManager manager;

    public CommandDispatcherBroadcastEndpointFactory(CommandDispatcherFactory factory, String name) {
        this.factory = factory;
        this.name = name;
        this.manager = BROADCAST_MANAGERS.computeIfAbsent(name, key -> new QueueBroadcastManager());
    }

    @Override
    public BroadcastEndpoint createBroadcastEndpoint() throws Exception {
        return new CommandDispatcherBroadcastEndpoint(this.factory, this.name, this.manager);
    }
}
